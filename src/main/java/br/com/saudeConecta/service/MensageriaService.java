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

    /**
     * Registra uma mensagem com falha de envio na tabela de mensageria.
     * Utilizado quando o envio de email falha após todas as tentativas.
     *
     * @param organizacaoId              ID da organização
     * @param destinatarioProfissionalId ID do profissional destinatário (pode ser nulo)
     * @param destinatarioEmail          Email do destinatário
     * @param destinatarioNome           Nome do destinatário
     * @param assunto                    Assunto da mensagem
     * @param corpoMensagem              Corpo completo da mensagem (template HTML)
     * @param tipoMensagem               Tipo da mensagem
     * @param erroDetalhe                Detalhe do erro ocorrido
     * @param tentativas                 Número de tentativas realizadas
     */
    @Transactional
    public void registrarFalhaEnvio(
            Long organizacaoId,
            Long destinatarioProfissionalId,
            String destinatarioEmail,
            String destinatarioNome,
            String assunto,
            String corpoMensagem,
            TipoMensagem tipoMensagem,
            String erroDetalhe,
            int tentativas) {

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + organizacaoId));

        Profissional profissional = null;
        if (destinatarioProfissionalId != null) {
            profissional = profissionalRepository.findById(destinatarioProfissionalId).orElse(null);
        }

        Mensageria mensageria = Mensageria.builder()
                .organizacao(organizacao)
                .destinatarioProfissional(profissional)
                .destinatarioEmail(destinatarioEmail)
                .destinatarioNome(destinatarioNome)
                .assunto(assunto)
                .corpoMensagem(corpoMensagem)
                .tipoMensagem(tipoMensagem)
                .status(StatusMensagem.FALHOU)
                .erroDetalhe(erroDetalhe)
                .tentativas(tentativas)
                .adminNotificado(false)
                .build();

        mensageriaRepository.save(mensageria);
        log.warn("Falha de envio registrada na mensageria. Destinatário: {}, Tipo: {}", destinatarioEmail, tipoMensagem);
    }

    /**
     * Registra uma mensagem enviada com sucesso na tabela de mensageria.
     * Utilizado para manter histórico completo de envios.
     *
     * @param organizacaoId              ID da organização
     * @param destinatarioProfissionalId ID do profissional destinatário (pode ser nulo)
     * @param destinatarioEmail          Email do destinatário
     * @param destinatarioNome           Nome do destinatário
     * @param assunto                    Assunto da mensagem
     * @param corpoMensagem              Corpo completo da mensagem (template HTML)
     * @param tipoMensagem               Tipo da mensagem
     * @param tentativas                 Número de tentativas até o sucesso
     */
    @Transactional
    public void registrarEnvioSucesso(
            Long organizacaoId,
            Long destinatarioProfissionalId,
            String destinatarioEmail,
            String destinatarioNome,
            String assunto,
            String corpoMensagem,
            TipoMensagem tipoMensagem,
            int tentativas) {

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada: " + organizacaoId));

        Profissional profissional = null;
        if (destinatarioProfissionalId != null) {
            profissional = profissionalRepository.findById(destinatarioProfissionalId).orElse(null);
        }

        Mensageria mensageria = Mensageria.builder()
                .organizacao(organizacao)
                .destinatarioProfissional(profissional)
                .destinatarioEmail(destinatarioEmail)
                .destinatarioNome(destinatarioNome)
                .assunto(assunto)
                .corpoMensagem(corpoMensagem)
                .tipoMensagem(tipoMensagem)
                .status(StatusMensagem.ENVIADO)
                .erroDetalhe(null)
                .tentativas(tentativas)
                .adminNotificado(true) // Sucesso não precisa notificar admin
                .build();

        mensageriaRepository.save(mensageria);
        log.info("Envio bem-sucedido registrado na mensageria. Destinatário: {}, Tipo: {}", destinatarioEmail, tipoMensagem);
    }

    /**
     * Lista todas as mensagens da organização com paginação e filtros opcionais.
     *
     * @param status   Filtro por status (pode ser nulo)
     * @param tipo     Filtro por tipo de mensagem (pode ser nulo)
     * @param pageable Configuração de paginação
     * @return Página de mensagens
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public Page<MensageriaResponse> listarMensagens(StatusMensagem status, TipoMensagem tipo, Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return mensageriaRepository
                .findByOrganizacaoIdWithFilters(orgId, status, tipo, pageable)
                .map(MensageriaResponse::fromEntity);
    }

    /**
     * Busca mensagens com falha ainda não notificadas ao administrador.
     *
     * @return Lista de mensagens com falha pendentes de notificação
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<MensageriaResponse> listarFalhasPendentesNotificacao() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return mensageriaRepository
                .findByOrganizacao_IdAndAdminNotificadoFalseAndStatus(orgId, StatusMensagem.FALHOU)
                .stream()
                .map(MensageriaResponse::fromEntity)
                .toList();
    }

    /**
     * Marca uma mensagem como notificada ao administrador.
     *
     * @param mensageriaId ID da mensagem
     */
    @RequiresTenant
    @Transactional
    public void marcarComoNotificado(Long mensageriaId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (!mensageria.getOrganizacaoId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        mensageria.setAdminNotificado(true);
        mensageria.setStatus(StatusMensagem.RENOTIFICADO);
        mensageriaRepository.save(mensageria);
        log.info("Mensagem {} marcada como notificada ao admin.", mensageriaId);
    }

    /**
     * Retorna contagem de mensagens com falha não notificadas (para badge de alerta).
     *
     * @return Quantidade de falhas pendentes
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public long contarFalhasPendentes() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return mensageriaRepository.countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(orgId, StatusMensagem.FALHOU);
    }

    /**
     * Busca uma mensagem específica por ID dentro da organização.
     *
     * @param mensageriaId ID da mensagem
     * @return DTO da mensagem
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public MensageriaResponse buscarPorId(Long mensageriaId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        Mensageria mensageria = mensageriaRepository.findById(mensageriaId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada: " + mensageriaId));

        if (!mensageria.getOrganizacaoId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado à mensagem: " + mensageriaId);
        }

        return MensageriaResponse.fromEntity(mensageria);
    }
}
