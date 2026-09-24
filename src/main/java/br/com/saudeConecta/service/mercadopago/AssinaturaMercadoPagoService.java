package br.com.saudeConecta.service.mercadopago;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.LinkPagamentoResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Regras de negócio da assinatura recorrente no Mercado Pago.
 * Cuida da geração do link de pagamento, do cancelamento e da reconciliação
 * de status entre o Mercado Pago e a base local.
 */
@Service
public class AssinaturaMercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaMercadoPagoService.class);
    private static final String PREFIXO_EXTERNAL_REFERENCE = "ASSINATURA-";

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties properties;

    public AssinaturaMercadoPagoService(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties properties) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.mercadoPagoClient = mercadoPagoClient;
        this.properties = properties;
    }

    /**
     * Gera (ou reaproveita) o link do checkout hospedado para a organização logada.
     *
     * @param organizacaoId ID da organização
     * @return dados do checkout, incluindo o init_point
     */
    @Transactional
    public LinkPagamentoResponse gerarLinkPagamento(Long organizacaoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        return gerarLinkPagamento(assinatura);
    }

    /**
     * Gera (ou reaproveita) o link do checkout hospedado para uma assinatura.
     * Reutiliza o preapproval existente enquanto ele ainda estiver pendente,
     * evitando criar assinaturas duplicadas no Mercado Pago.
     *
     * @param assinatura assinatura do tenant
     * @return dados do checkout, incluindo o init_point
     */
    @Transactional
    public LinkPagamentoResponse gerarLinkPagamento(AssinaturaTenant assinatura) {
        if (assinatura.isCortesia()) {
            throw new BusinessException(
                    "Assinaturas de cortesia não possuem cobrança no Mercado Pago",
                    HttpStatus.CONFLICT);
        }
        if (StatusAssinatura.ATIVA.equals(assinatura.getStatus()) && assinatura.temPreapproval()) {
            throw new BusinessException("Esta assinatura já está ativa", HttpStatus.CONFLICT);
        }

        if (assinatura.temPreapproval() && assinatura.getMpInitPoint() != null
                && MercadoPagoClient.STATUS_PENDING.equals(assinatura.getMpStatus())) {
            log.info("Reaproveitando preapproval pendente {} da assinatura {}",
                    assinatura.getMpPreapprovalId(), assinatura.getId());
            return montarResposta(assinatura);
        }

        String payerEmail = resolverPayerEmail(assinatura);
        String reason = properties.getReasonPrefix() + " — Plano "
                + assinatura.getPlanoAssinatura().getNome();

        PreapprovalResponse preapproval = mercadoPagoClient.criarPreapprovalPendente(
                reason,
                PREFIXO_EXTERNAL_REFERENCE + assinatura.getId(),
                payerEmail,
                assinatura.getValorMensal());

        if (preapproval == null || preapproval.initPoint() == null) {
            throw new BusinessException(
                    "O Mercado Pago não retornou o link de pagamento. Tente novamente.",
                    HttpStatus.BAD_GATEWAY);
        }

        assinatura.setMpPreapprovalId(preapproval.id());
        assinatura.setMpStatus(preapproval.status());
        assinatura.setMpInitPoint(preapproval.initPoint());
        assinatura.setMpPayerEmail(payerEmail);
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
        assinaturaTenantRepository.save(assinatura);

        log.info("Preapproval {} criado para assinatura {} (org {})",
                preapproval.id(), assinatura.getId(), assinatura.getOrganizacao().getId());

        return montarResposta(assinatura);
    }

    /**
     * Cancela a assinatura a pedido do próprio cliente.
     * O cancelamento é propagado ao Mercado Pago antes de ser refletido localmente.
     *
     * @param organizacaoId ID da organização
     */
    @Transactional
    public void cancelarPeloCliente(Long organizacaoId) {
        AssinaturaTenant assinatura = assinaturaTenantRepository
                .findAssinaturaAtivaByOrganizacaoId(organizacaoId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma assinatura ativa encontrada para a organização",
                        HttpStatus.NOT_FOUND));

        cancelar(assinatura, OrigemCancelamento.CLIENTE);
    }

    /**
     * Cancela a assinatura local e, quando houver, também no Mercado Pago.
     *
     * @param assinatura assinatura a cancelar
     * @param origem     quem originou o cancelamento
     */
    @Transactional
    public void cancelar(AssinaturaTenant assinatura, OrigemCancelamento origem) {
        if (StatusAssinatura.CANCELADA.equals(assinatura.getStatus())) {
            log.info("Assinatura {} já cancelada, ignorando", assinatura.getId());
            return;
        }

        if (assinatura.temPreapproval() && !MercadoPagoClient.STATUS_CANCELLED.equals(assinatura.getMpStatus())) {
            PreapprovalResponse resposta = mercadoPagoClient.cancelarPreapproval(assinatura.getMpPreapprovalId());
            assinatura.setMpStatus(resposta != null ? resposta.status() : MercadoPagoClient.STATUS_CANCELLED);
        }

        assinatura.setStatus(StatusAssinatura.CANCELADA);
        assinatura.setDataCancelamento(LocalDateTime.now());
        assinatura.setCanceladoPor(origem);
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
        assinaturaTenantRepository.save(assinatura);

        log.warn("Assinatura {} CANCELADA por {} (org {})",
                assinatura.getId(), origem, assinatura.getOrganizacao().getId());
    }

    /**
     * Pausa a cobrança no Mercado Pago quando o tenant é suspenso por inadimplência.
     * Falhas de comunicação não interrompem a suspensão local.
     *
     * @param assinatura assinatura suspensa
     */
    @Transactional
    public void pausar(AssinaturaTenant assinatura) {
        if (!assinatura.temPreapproval() || MercadoPagoClient.STATUS_PAUSED.equals(assinatura.getMpStatus())) {
            return;
        }
        try {
            PreapprovalResponse resposta = mercadoPagoClient.pausarPreapproval(assinatura.getMpPreapprovalId());
            assinatura.setMpStatus(resposta != null ? resposta.status() : MercadoPagoClient.STATUS_PAUSED);
            assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
            assinaturaTenantRepository.save(assinatura);
        } catch (BusinessException e) {
            log.error("Não foi possível pausar o preapproval {}: {}",
                    assinatura.getMpPreapprovalId(), e.getMessage());
        }
    }

    /**
     * Reconcilia o status local com o status atual no Mercado Pago.
     * Rede de segurança para webhooks perdidos.
     *
     * @param assinatura assinatura com preapproval vinculado
     * @return true se houve alteração de status
     */
    @Transactional
    public boolean sincronizar(AssinaturaTenant assinatura) {
        if (!assinatura.temPreapproval()) {
            return false;
        }

        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(assinatura.getMpPreapprovalId());
        if (remoto == null || remoto.status() == null) {
            return false;
        }

        boolean mudou = !remoto.status().equals(assinatura.getMpStatus());
        assinatura.setMpStatus(remoto.status());
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());

        StatusAssinatura statusLocal = mapearStatus(remoto.status());
        if (statusLocal != null && statusLocal != assinatura.getStatus()) {
            log.warn("Reconciliação: assinatura {} passou de {} para {} (MP: {})",
                    assinatura.getId(), assinatura.getStatus(), statusLocal, remoto.status());
            assinatura.setStatus(statusLocal);
            mudou = true;
        }

        assinaturaTenantRepository.save(assinatura);
        return mudou;
    }

    /**
     * Traduz o status do Mercado Pago para o status interno da assinatura.
     *
     * @param mpStatus status retornado pelo Mercado Pago
     * @return status interno correspondente ou null quando não há equivalência
     */
    public StatusAssinatura mapearStatus(String mpStatus) {
        if (mpStatus == null) {
            return null;
        }
        return switch (mpStatus) {
            case MercadoPagoClient.STATUS_AUTHORIZED -> StatusAssinatura.ATIVA;
            case MercadoPagoClient.STATUS_PAUSED -> StatusAssinatura.SUSPENSA;
            case MercadoPagoClient.STATUS_CANCELLED -> StatusAssinatura.CANCELADA;
            default -> null;
        };
    }

    /**
     * Determina o e-mail do pagador: o do administrador owner da organização.
     * Esse e-mail precisa corresponder à conta Mercado Pago usada no checkout,
     * caso contrário o pagamento é recusado pelo próprio Mercado Pago.
     */
    private String resolverPayerEmail(AssinaturaTenant assinatura) {
        if (assinatura.getMpPayerEmail() != null && !assinatura.getMpPayerEmail().isBlank()) {
            return assinatura.getMpPayerEmail();
        }

        return adminOrganizacaoRepository
                .findOwnerByOrganizacaoId(assinatura.getOrganizacao().getId())
                .map(admin -> admin.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .orElseThrow(() -> new BusinessException(
                        "Organização sem e-mail de administrador cadastrado para o pagamento",
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private LinkPagamentoResponse montarResposta(AssinaturaTenant assinatura) {
        return new LinkPagamentoResponse(
                assinatura.getMpPreapprovalId(),
                assinatura.getMpInitPoint(),
                assinatura.getMpPayerEmail(),
                assinatura.getValorMensal());
    }
}
