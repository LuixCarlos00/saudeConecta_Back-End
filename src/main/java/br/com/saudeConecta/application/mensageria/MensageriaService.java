package br.com.saudeConecta.application.mensageria;

import br.com.saudeConecta.config.logging.MascaraLog;

import br.com.saudeConecta.application.mensageria.dto.MensageriaResponse;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MensageriaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;

/**
 * Service responsável pelo gerenciamento de mensagens do sistema.
 * Registra mensagens enviadas, falhas de envio e notificações ao administrador.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MensageriaService {

    private final MensageriaRepository mensageriaRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final SecretariaRepository secretariaRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final TenantHelper tenantHelper;
    private final EmailNotificacaoService emailNotificacaoService;

    /**
     * Lista mensagens com paginação e filtros opcionais.
     * SuperAdmin (sem organização) vê mensagens globais de todas as organizações.
     * Admin Org vê apenas mensagens da própria organização.
     *
     * @param status   Filtro por status (pode ser nulo)
     * @param tipo     Filtro por tipo de mensagem (pode ser nulo)
     * @param pageable Configuração de paginação
     * @return Página de mensagens
     */
    @Cacheable(
        value = "mensageria-lista",
        key = "(@tenantHelper.getCurrentTenantIdOrNull() != null ? 'org-' + @tenantHelper.getCurrentTenantIdOrNull() : (@tenantHelper.isSuperAdmin() ? 'super-admin' : 'sem-tenant'))"
            + " + '-s:' + (#status != null ? #status.name() : 'ALL')"
            + " + '-t:' + (#tipo != null ? #tipo.name() : 'ALL')"
            + " + '-p:' + #pageable.pageNumber + '-sz:' + #pageable.pageSize"
    )
    @Transactional(readOnly = true)
    public Page<MensageriaResponse> listarMensagens(StatusMensagem status, TipoMensagem tipo, Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        if (orgId == null) {
            log.debug("mensageria.listar escopo=SUPER_ADMIN");
            return mensageriaRepository
                    .findAllWithFilters(status, tipo, pageable)
                    .map(MensageriaResponse::fromEntity);
        }
        return mensageriaRepository
                .findByOrganizacaoIdWithFilters(orgId, status, tipo, pageable)
                .map(MensageriaResponse::fromEntity);
    }

    /**
     * Marca uma mensagem como notificada ao administrador.
     * SuperAdmin pode marcar mensagens de qualquer organização.
     *
     * @param mensageriaId ID da mensagem
     */
    @CacheEvict(value = {"mensageria-contagem", "mensageria-lista"}, allEntries = true)
    @Transactional
    public void marcarComoNotificado(Long mensageriaId) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (orgId != null && !orgId.equals(mensageria.getOrganizacaoId())) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        mensageria.setAdminNotificado(true);
        mensageria.setStatus(StatusMensagem.RENOTIFICADO);
        mensageriaRepository.save(mensageria);
        log.info("mensageria.notificar ok mensageriaId={}", mensageriaId);
    }

    /**
     * Retorna contagem de mensagens com falha não notificadas (para badge de alerta).
     * SuperAdmin vê contagem global de todas as organizações.
     *
     * @return Quantidade de falhas pendentes
     */
    @Cacheable(value = "mensageria-contagem", key = "(@tenantHelper.getCurrentTenantIdOrNull() != null ? 'org-' + @tenantHelper.getCurrentTenantIdOrNull() : (@tenantHelper.isSuperAdmin() ? 'super-admin' : 'sem-tenant')) + '-falhas-pendentes'")
    @Transactional(readOnly = true)
    public long contarFalhasPendentes() {
        Long orgId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        if (orgId == null) {
            return mensageriaRepository.countByAdminNotificadoFalseAndStatus(StatusMensagem.FALHOU);
        }
        return mensageriaRepository.countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(orgId, StatusMensagem.FALHOU);
    }

    /**
     * Reenvia uma mensagem registrada na mensageria.
     * Busca o email atualizado da entidade de domínio (Profissional, Secretaria ou AdminOrganizacao)
     * para garantir que o email mais recente seja utilizado.
     * Atualiza o status e incrementa o número de tentativas.
     *
     * @param mensageriaId ID da mensagem a ser reenviada
     */
    @CacheEvict(value = {"mensageria-contagem", "mensageria-lista"}, allEntries = true)
    @Transactional
    public void reenviarMensagem(Long mensageriaId) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (orgId != null && !orgId.equals(mensageria.getOrganizacaoId())) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        // Buscar email atualizado da entidade de domínio
        String emailAtualizado = obterEmailAtualizadoDoDominio(mensageria);
        
        log.info("mensageria.reenviar inicio mensageriaId={} email={}", mensageriaId, MascaraLog.email(emailAtualizado));

        mensageria.setStatus(StatusMensagem.PENDENTE);
        mensageria.setErroDetalhe(null);
        mensageriaRepository.save(mensageria);

        emailNotificacaoService.reenviarEmail(
                mensageria.getId(),
                emailAtualizado,
                mensageria.getAssunto(),
                mensageria.getCorpoMensagem()
        );

        log.debug("mensageria.reenviar delegado mensageriaId={}", mensageriaId);
    }

    /**
     * Obtém o email atualizado da entidade de domínio correspondente.
     * Busca em Profissional, Secretaria ou AdminOrganizacao conforme o tipo de mensagem.
     *
     * @param mensageria Mensagem com informações do destinatário
     * @return Email atualizado da entidade de domínio
     * @throws IllegalStateException se não conseguir encontrar o email atualizado
     */
    private String obterEmailAtualizadoDoDominio(Mensageria mensageria) {
        Long entidadeId = mensageria.getDestinatarioEntidadeId();
        TipoMensagem tipo = mensageria.getTipoMensagem();

        // Tentar buscar por Profissional (Clínico/Dentista)
        if (entidadeId != null && tipo == TipoMensagem.EMAIL_CREDENCIAIS_CLINICO) {
            return profissionalRepository.findById(entidadeId)
                    .map(Profissional::getEmail)
                    .orElseThrow(() -> new IllegalStateException(
                            "Profissional não encontrado para ID: " + entidadeId));
        }

        // Tentar buscar por Secretaria
        if (entidadeId != null && tipo == TipoMensagem.EMAIL_CREDENCIAIS_SECRETARIA) {
            return secretariaRepository.findById(entidadeId)
                    .map(s -> s.getEmail())
                    .orElseThrow(() -> new IllegalStateException(
                            "Secretária não encontrada para ID: " + entidadeId));
        }

        // Tentar buscar por AdminOrganizacao
        if (entidadeId != null && tipo == TipoMensagem.EMAIL_CREDENCIAIS_ADMINISTRADOR) {
            return adminOrganizacaoRepository.findById(entidadeId)
                    .map(a -> a.getEmail())
                    .orElseThrow(() -> new IllegalStateException(
                            "Administrador não encontrado para ID: " + entidadeId));
        }

        // Tentar buscar por Paciente
        if (entidadeId != null && tipo == TipoMensagem.EMAIL_CREDENCIAIS_PACIENTE) {
            return pacienteRepository.findById(entidadeId)
                    .map(p -> p.getPaciEmail())
                    .orElseThrow(() -> new IllegalStateException(
                            "Paciente não encontrado para ID: " + entidadeId));
        }

        // Fallback: usar email armazenado na mensageria para outros tipos
        log.warn("mensageria.reenviar ignorado motivo=EMAIL_DOMINIO_INDISPONIVEL mensageriaId={} acao=USA_EMAIL_ARMAZENADO",
                mensageria.getId());
        return mensageria.getDestinatarioEmail();
    }
}
