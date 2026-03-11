package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.MensageriaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.presentation.dto.mensageria.MensageriaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Utiliza os dados armazenados (email, assunto, corpo) para disparar o email novamente.
     * Atualiza o status e incrementa o número de tentativas.
     *
     * @param mensageriaId ID da mensagem a ser reenviada
     */
    @Transactional
    public void reenviarMensagem(Long mensageriaId) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (orgId != null && !mensageria.getOrganizacaoId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        log.info("Reenviando mensagem ID: {} para: {}", mensageriaId, mensageria.getDestinatarioEmail());

        mensageria.setStatus(StatusMensagem.PENDENTE);
        mensageria.setErroDetalhe(null);
        mensageriaRepository.save(mensageria);

        emailNotificacaoService.reenviarEmail(
                mensageria.getId(),
                mensageria.getDestinatarioEmail(),
                mensageria.getAssunto(),
                mensageria.getCorpoMensagem()
        );

        log.info("Reenvio delegado ao EmailNotificacaoService. Mensagem ID: {}", mensageriaId);
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
            case EMAIL_RECUPERACAO_SENHA -> "recuperacao";
            case EMAIL_GENERICO -> "generico";
        };
    }
}
