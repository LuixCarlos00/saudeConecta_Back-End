package br.com.saudeConecta.service.mercadopago;

import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.service.AssinaturaTenantService;
import br.com.saudeConecta.service.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Processa as notificações do Mercado Pago de forma idempotente.
 *
 * O Mercado Pago reenvia a mesma notificação até receber uma resposta 2xx, então
 * cada evento é persistido em {@code mp_webhook_evento} com UNIQUE (tipo, data_id)
 * antes de qualquer efeito colateral.
 */
@Service
public class MercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    public static final String TOPICO_PREAPPROVAL = "subscription_preapproval";
    public static final String TOPICO_AUTHORIZED_PAYMENT = "subscription_authorized_payment";

    private static final String PAGAMENTO_APROVADO = "approved";
    private static final String PAGAMENTO_RECUSADO = "rejected";

    private final MpWebhookEventoRepository eventoRepository;
    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final CobrancaTenantRepository cobrancaTenantRepository;
    private final AssinaturaTenantService assinaturaTenantService;
    private final MercadoPagoClient mercadoPagoClient;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    private final TransactionTemplate transactionTemplate;

    public MercadoPagoWebhookService(
            MpWebhookEventoRepository eventoRepository,
            AssinaturaTenantRepository assinaturaTenantRepository,
            CobrancaTenantRepository cobrancaTenantRepository,
            AssinaturaTenantService assinaturaTenantService,
            MercadoPagoClient mercadoPagoClient,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService,
            PlatformTransactionManager transactionManager) {
        this.eventoRepository = eventoRepository;
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.cobrancaTenantRepository = cobrancaTenantRepository;
        this.assinaturaTenantService = assinaturaTenantService;
        this.mercadoPagoClient = mercadoPagoClient;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Ponto de entrada assíncrono do webhook.
     * O controller responde 200 imediatamente — o Mercado Pago exige resposta em até 22s.
     *
     * @param tipo    tópico notificado
     * @param dataId  identificador do recurso (data.id)
     * @param payload corpo bruto da notificação, guardado para auditoria
     */
    @Async
    public void processarAssincrono(String tipo, String dataId, String payload) {
        try {
            processar(tipo, dataId, payload);
        } catch (Exception e) {
            log.error("Erro ao processar webhook {} data.id={}: {}", tipo, dataId, e.getMessage(), e);
        }
    }

    /**
     * Processa a notificação garantindo idempotência por (tipo, data.id).
     *
     * O registro do evento, os efeitos colaterais e a gravação do resultado usam
     * transações separadas: assim uma falha no processamento faz rollback apenas
     * dos efeitos, preservando o evento com a mensagem de erro para reprocessamento.
     *
     * @param tipo    tópico notificado
     * @param dataId  identificador do recurso
     * @param payload corpo bruto da notificação
     */
    public void processar(String tipo, String dataId, String payload) {
        MpWebhookEvento evento = transactionTemplate.execute(status -> registrarEvento(tipo, dataId, payload));
        if (evento == null) {
            log.info("Webhook {} data.id={} já processado, ignorando", tipo, dataId);
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> despachar(tipo, dataId));
            evento.marcarProcessado();

        } catch (Exception e) {
            log.error("Falha ao processar webhook {} data.id={}: {}", tipo, dataId, e.getMessage(), e);
            evento.marcarErro(e.getMessage());
        }

        transactionTemplate.executeWithoutResult(status -> eventoRepository.save(evento));
    }

    private void despachar(String tipo, String dataId) {
        switch (tipo) {
            case TOPICO_PREAPPROVAL -> processarPreapproval(dataId);
            case TOPICO_AUTHORIZED_PAYMENT -> processarPagamentoAutorizado(dataId);
            default -> log.info("Tópico não tratado pelo Saúde Conecta: {}", tipo);
        }
    }

    /**
     * Persiste o evento se ele ainda não existir ou se a tentativa anterior falhou.
     *
     * @return o evento a processar, ou null quando já foi processado com sucesso
     */
    private MpWebhookEvento registrarEvento(String tipo, String dataId, String payload) {
        Optional<MpWebhookEvento> existente = eventoRepository.findByTipoAndDataId(tipo, dataId);
        if (existente.isPresent()) {
            MpWebhookEvento evento = existente.get();
            return Boolean.TRUE.equals(evento.getProcessado()) ? null : evento;
        }

        try {
            return eventoRepository.save(MpWebhookEvento.builder()
                    .tipo(tipo)
                    .dataId(dataId)
                    .payload(payload)
                    .processado(false)
                    .recebidoEm(LocalDateTime.now())
                    .build());

        } catch (DataIntegrityViolationException e) {
            // Corrida entre duas entregas simultâneas da mesma notificação
            log.info("Evento {} data.id={} já registrado concorrentemente", tipo, dataId);
            return null;
        }
    }

    /**
     * Reflete localmente a mudança de status da assinatura no Mercado Pago.
     */
    private void processarPreapproval(String preapprovalId) {
        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(preapprovalId);
        if (remoto == null) {
            log.warn("Preapproval {} não encontrado no Mercado Pago", preapprovalId);
            return;
        }

        AssinaturaTenant assinatura = localizarAssinatura(preapprovalId, remoto.externalReference());
        if (assinatura == null) {
            return;
        }

        assinatura.setMpPreapprovalId(remoto.id());
        assinatura.setMpStatus(remoto.status());
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());

        StatusAssinatura novoStatus = assinaturaMercadoPagoService.mapearStatus(remoto.status());
        if (novoStatus == null) {
            assinaturaTenantRepository.save(assinatura);
            return;
        }

        if (StatusAssinatura.ATIVA.equals(novoStatus)) {
            assinatura.setStatus(StatusAssinatura.ATIVA);
            assinatura.setDataVencimento(java.time.LocalDate.now().plusMonths(1));
            assinatura.setDataProximaCobranca(java.time.LocalDate.now().plusMonths(1));
        } else {
            assinatura.setStatus(novoStatus);
            if (StatusAssinatura.CANCELADA.equals(novoStatus)) {
                assinatura.setDataCancelamento(LocalDateTime.now());
                assinatura.setCanceladoPor(OrigemCancelamento.MERCADO_PAGO);
            }
        }

        assinaturaTenantRepository.save(assinatura);
        log.info("Assinatura {} atualizada para {} via webhook (MP: {})",
                assinatura.getId(), assinatura.getStatus(), remoto.status());
    }

    /**
     * Lança a fatura mensal cobrada pelo Mercado Pago como CobrancaTenant.
     */
    private void processarPagamentoAutorizado(String authorizedPaymentId) {
        AuthorizedPaymentResponse fatura = mercadoPagoClient.buscarAuthorizedPayment(authorizedPaymentId);
        if (fatura == null) {
            log.warn("Authorized payment {} não encontrado no Mercado Pago", authorizedPaymentId);
            return;
        }

        AssinaturaTenant assinatura = localizarAssinatura(fatura.preapprovalId(), null);
        if (assinatura == null) {
            return;
        }

        String statusPagamento = fatura.statusPagamento();
        if (statusPagamento == null) {
            log.info("Fatura {} ainda sem pagamento processado (status={})", authorizedPaymentId, fatura.status());
            return;
        }

        CobrancaTenant cobranca = cobrancaTenantRepository
                .findByMpAuthorizedPaymentId(authorizedPaymentId)
                .orElseGet(() -> novaCobranca(assinatura, fatura, authorizedPaymentId));

        if (PAGAMENTO_APROVADO.equals(statusPagamento)) {
            if (cobranca.isPago()) {
                log.info("Fatura {} já registrada como paga, ignorando", authorizedPaymentId);
                return;
            }
            cobranca.setStatus(StatusCobranca.PAGO);
            cobranca.setMotivoRecusa(null);
            cobranca.setDataPagamento(LocalDateTime.now());
            cobrancaTenantRepository.save(cobranca);

            assinaturaTenantService.renovarAposPagamento(assinatura.getId());
            log.info("Pagamento aprovado da assinatura {} (fatura {})", assinatura.getId(), authorizedPaymentId);

        } else if (PAGAMENTO_RECUSADO.equals(statusPagamento)) {
            cobranca.setStatus(StatusCobranca.RECUSADO);
            cobranca.setMotivoRecusa(fatura.motivoRecusa());
            cobrancaTenantRepository.save(cobranca);

            assinaturaTenantService.marcarInadimplente(assinatura.getId());
            log.warn("Pagamento recusado da assinatura {} (fatura {}, motivo {})",
                    assinatura.getId(), authorizedPaymentId, fatura.motivoRecusa());

        } else {
            cobrancaTenantRepository.save(cobranca);
            log.info("Fatura {} em status intermediário: {}", authorizedPaymentId, statusPagamento);
        }
    }

    private CobrancaTenant novaCobranca(AssinaturaTenant assinatura, AuthorizedPaymentResponse fatura,
                                        String authorizedPaymentId) {
        BigDecimal valor = fatura.transactionAmount() != null
                ? fatura.transactionAmount()
                : assinatura.getValorMensal();

        return CobrancaTenant.builder()
                .assinaturaTenant(assinatura)
                .organizacao(assinatura.getOrganizacao())
                .valorTotal(valor)
                .status(StatusCobranca.PENDENTE)
                .formaPagamento(FormaPagamento.MERCADO_PAGO)
                .mpAuthorizedPaymentId(authorizedPaymentId)
                .mpPaymentId(fatura.payment() != null && fatura.payment().id() != null
                        ? String.valueOf(fatura.payment().id()) : null)
                .build();
    }

    /**
     * Localiza a assinatura pelo preapproval e, como fallback, pela external_reference
     * (cobre o caso do primeiro webhook chegar antes da gravação do preapproval).
     */
    private AssinaturaTenant localizarAssinatura(String preapprovalId, String externalReference) {
        if (preapprovalId != null) {
            Optional<AssinaturaTenant> porPreapproval =
                    assinaturaTenantRepository.findByMpPreapprovalId(preapprovalId);
            if (porPreapproval.isPresent()) {
                return porPreapproval.get();
            }
        }

        Long assinaturaId = extrairIdDaExternalReference(externalReference);
        if (assinaturaId != null) {
            Optional<AssinaturaTenant> porId = assinaturaTenantRepository.findById(assinaturaId);
            if (porId.isPresent()) {
                return porId.get();
            }
        }

        log.warn("Nenhuma assinatura local encontrada para preapproval={} externalReference={}",
                preapprovalId, externalReference);
        return null;
    }

    private Long extrairIdDaExternalReference(String externalReference) {
        if (externalReference == null || !externalReference.startsWith("ASSINATURA-")) {
            return null;
        }
        try {
            return Long.valueOf(externalReference.substring("ASSINATURA-".length()));
        } catch (NumberFormatException e) {
            log.warn("external_reference inválida recebida do Mercado Pago: {}", externalReference);
            return null;
        }
    }
}
