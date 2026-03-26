package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.MensageriaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.presentation.dto.mensageria.MensageriaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saudeConecta.email.EmailNotificacaoService;

import java.util.List;

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
        key = "(@tenantHelper.getCurrentTenantIdOrNull() != null ? 'org-' + @tenantHelper.getCurrentTenantIdOrNull() : 'super-admin')"
            + " + '-s:' + (#status != null ? #status.name() : 'ALL')"
            + " + '-t:' + (#tipo != null ? #tipo.name() : 'ALL')"
            + " + '-p:' + #pageable.pageNumber + '-sz:' + #pageable.pageSize"
    )
    @Transactional(readOnly = true)
    public Page<MensageriaResponse> listarMensagens(StatusMensagem status, TipoMensagem tipo, Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        if (orgId == null) {
            log.info("SUPER_ADMIN: listando mensagens globais");
            return mensageriaRepository
                    .findAllWithFilters(status, tipo, pageable)
                    .map(MensageriaResponse::fromEntity);
        }
        return mensageriaRepository
                .findByOrganizacaoIdWithFilters(orgId, status, tipo, pageable)
                .map(MensageriaResponse::fromEntity);
    }

    /**
     * Busca mensagens com falha ainda não notificadas ao administrador.
     * SuperAdmin vê falhas de todas as organizações.
     *
     * @return Lista de mensagens com falha pendentes de notificação
     */
    @Transactional(readOnly = true)
    public List<MensageriaResponse> listarFalhasPendentesNotificacao() {
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        if (orgId == null) {
            log.info("SUPER_ADMIN: listando falhas pendentes globais");
            return mensageriaRepository
                    .findByAdminNotificadoFalseAndStatus(StatusMensagem.FALHOU)
                    .stream()
                    .map(MensageriaResponse::fromEntity)
                    .toList();
        }
        return mensageriaRepository
                .findByOrganizacao_IdAndAdminNotificadoFalseAndStatus(orgId, StatusMensagem.FALHOU)
                .stream()
                .map(MensageriaResponse::fromEntity)
                .toList();
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
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (orgId != null && !mensageria.getOrganizacaoId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        mensageria.setAdminNotificado(true);
        mensageria.setStatus(StatusMensagem.RENOTIFICADO);
        mensageriaRepository.save(mensageria);
        log.info("Mensagem {} marcada como notificada ao admin.", mensageriaId);
    }

    /**
     * Retorna contagem de mensagens com falha não notificadas (para badge de alerta).
     * SuperAdmin vê contagem global de todas as organizações.
     *
     * @return Quantidade de falhas pendentes
     */
    @Cacheable(value = "mensageria-contagem", key = "(@tenantHelper.getCurrentTenantIdOrNull() != null ? 'org-' + @tenantHelper.getCurrentTenantIdOrNull() : 'super-admin') + '-falhas-pendentes'")
    @Transactional(readOnly = true)
    public long contarFalhasPendentes() {
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        if (orgId == null) {
            return mensageriaRepository.countByAdminNotificadoFalseAndStatus(StatusMensagem.FALHOU);
        }
        return mensageriaRepository.countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(orgId, StatusMensagem.FALHOU);
    }

    /**
     * Busca uma mensagem específica por ID.
     * SuperAdmin pode acessar mensagens de qualquer organização.
     *
     * @param mensageriaId ID da mensagem
     * @return DTO da mensagem
     */
    @Transactional(readOnly = true)
    public MensageriaResponse buscarPorId(Long mensageriaId) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (orgId != null && !mensageria.getOrganizacaoId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        return MensageriaResponse.fromEntity(mensageria);
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
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (orgId != null && !mensageria.getOrganizacaoId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        // Buscar email atualizado da entidade de domínio
        String emailAtualizado = obterEmailAtualizadoDoDominio(mensageria);
        
        log.info("Reenviando mensagem ID: {} para email atualizado: {}", mensageriaId, emailAtualizado);

        mensageria.setStatus(StatusMensagem.PENDENTE);
        mensageria.setErroDetalhe(null);
        mensageriaRepository.save(mensageria);

        emailNotificacaoService.reenviarEmail(
                mensageria.getId(),
                emailAtualizado,
                mensageria.getAssunto(),
                mensageria.getCorpoMensagem()
        );

        log.info("Reenvio delegado ao EmailNotificacaoService. Mensagem ID: {}", mensageriaId);
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
        log.warn("Nao foi possivel buscar email do dominio para mensagem ID: {}. Usando email armazenado.", 
                 mensageria.getId());
        return mensageria.getDestinatarioEmail();
    }

    /**
     * Converte o enum TipoMensagem para a String de tipo de usuário usada pelo EmailNotificacaoService.
     *
     * @param tipoMensagem tipo da mensagem armazenada
     * @return String do tipo de usuário
     */
    private String resolverTipoUsuario(TipoMensagem tipoMensagem) {
        return switch (tipoMensagem) {
            case EMAIL_CREDENCIAIS_CLINICO -> "medico";
            case EMAIL_CREDENCIAIS_SECRETARIA -> "secretaria";
            case EMAIL_CREDENCIAIS_ADMINISTRADOR -> "administrador";
            case EMAIL_CREDENCIAIS_PACIENTE -> "paciente";
            case EMAIL_RECUPERACAO_SENHA -> "recuperacao";
            case EMAIL_GENERICO -> "generico";
        };
    }
}
