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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Processa as notificações do Mercado Pago de forma idempotente.
 *
 * Cada notificação é persistida em {@code mp_webhook_evento} (UNIQUE tipo, data_id)
 * antes de qualquer efeito. O efeito é decidido sempre pelo estado atual do recurso
 * consultado no Mercado Pago, e o evento só é encerrado quando esse recurso chega a
 * um estado final — a mesma fatura é notificada de novo a cada mudança (sem pagamento
 * → recusada → aprovada) e nenhuma dessas transições pode ser descartada.
 *
 * Rede de recuperação de pagamentos aprovados: eventos que falharam (MP fora do ar,
 * conflito de versão persistente) são reprocessados por {@link #reprocessarEventosComErro()}
 * com backoff, e {@link #recuperarFaturasAprovadas(String)} busca no MP as faturas
 * aprovadas cuja notificação nunca chegou.
 */
@Service
public class MercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    public static final String TOPICO_PREAPPROVAL = "subscription_preapproval";
    public static final String TOPICO_AUTHORIZED_PAYMENT = "subscription_authorized_payment";

    private static final String PAGAMENTO_APROVADO = "approved";
    private static final String PAGAMENTO_RECUSADO = "rejected";

    /** Quantas vezes o processamento é reaplicado quando outra transação alterou a assinatura. */
    static final int MAX_TENTATIVAS_CONFLITO_VERSAO = 3;
    /** Eventos com erro mais antigos que isso deixam de ser reprocessados e são marcados como expirados. */
    static final int PRAZO_REPROCESSAMENTO_DIAS = 3;
    private static final Duration BACKOFF_BASE = Duration.ofMinutes(5);
    private static final Duration BACKOFF_MAXIMO = Duration.ofHours(6);
    private static final int TAMANHO_MAXIMO_ERRO = 500;
    private static final String PAYLOAD_RECUPERACAO = "{\"origem\":\"recuperacao-reconciliacao\"}";

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
     * O registro do evento e o processamento usam transações separadas: uma falha
     * no processamento desfaz apenas os efeitos, e o erro é gravado depois para
     * reprocessamento. Eventos já encerrados (estado final) são ignorados.
     *
     * @param tipo    tópico notificado
     * @param dataId  identificador do recurso
     * @param payload corpo bruto da notificação
     */
    public void processar(String tipo, String dataId, String payload) {
        MpWebhookEvento evento = transactionTemplate.execute(status -> registrarEvento(tipo, dataId, payload));
        if (evento == null) {
            log.info("Webhook {} data.id={} já encerrado, ignorando", tipo, dataId);
            return;
        }

        try {
            processarComReaplicacao(evento, tipo, dataId);
        } catch (Exception e) {
            log.error("Falha ao processar webhook {} data.id={}: {}", tipo, dataId, e.getMessage(), e);
            registrarFalha(evento, e);
        }
    }

    /**
     * Recupera pagamentos aprovados cujo webhook se perdeu: consulta as faturas do
     * preapproval no Mercado Pago e processa, pelo mesmo caminho do webhook (idempotente),
     * as aprovadas que ainda não têm cobrança PAGO local.
     *
     * Não pode ser chamado dentro de uma transação: cada fatura usa transações próprias.
     *
     * @param preapprovalId identificador do preapproval
     * @return quantas faturas aprovadas foram registradas agora
     */
    public int recuperarFaturasAprovadas(String preapprovalId) {
        int recuperadas = 0;
        for (AuthorizedPaymentResponse fatura : mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(preapprovalId)) {
            if (fatura == null || fatura.id() == null
                    || !PAGAMENTO_APROVADO.equals(fatura.statusPagamento())
                    || faturaRegistradaComoPaga(fatura.id())) {
                continue;
            }
            log.warn("Fatura aprovada {} do preapproval {} sem registro local (webhook perdido) — recuperando",
                    fatura.id(), preapprovalId);
            processar(TOPICO_AUTHORIZED_PAYMENT, fatura.id(), PAYLOAD_RECUPERACAO);
            if (faturaRegistradaComoPaga(fatura.id())) {
                recuperadas++;
            }
        }
        return recuperadas;
    }

    /**
     * Reprocessa os eventos que falharam e ainda estão no prazo, respeitando o backoff
     * de cada um. Antes, marca como expirados os que passaram do prazo sem sucesso
     * (param de ser tentados e ficam para tratamento manual).
     *
     * @return quantos eventos foram reprocessados nesta execução
     */
    public int reprocessarEventosComErro() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = agora.minusDays(PRAZO_REPROCESSAMENTO_DIAS);

        Integer expirados = transactionTemplate.execute(status -> eventoRepository.expirarComErroAntesDe(limite, agora));
        if (expirados != null && expirados > 0) {
            log.warn("{} evento(s) do Mercado Pago com erro expirado(s) após {} dias sem sucesso — tratar manualmente",
                    expirados, PRAZO_REPROCESSAMENTO_DIAS);
        }

        List<MpWebhookEvento> elegiveis = eventoRepository.findComErroParaReprocessar(limite, agora);
        for (MpWebhookEvento evento : elegiveis) {
            log.info("Reprocessando webhook {} data.id={} (tentativas anteriores: {}, último erro: {})",
                    evento.getTipo(), evento.getDataId(), evento.getTentativas(), evento.getErro());
            processar(evento.getTipo(), evento.getDataId(), evento.getPayload());
        }
        return elegiveis.size();
    }

    /**
     * Aplica o efeito do evento. Se outra transação alterou a mesma assinatura (lock
     * otimista), tudo é desfeito e reaplicado do zero — relendo evento, recurso no MP e
     * assinatura — até {@link #MAX_TENTATIVAS_CONFLITO_VERSAO} vezes.
     */
    private void processarComReaplicacao(MpWebhookEvento evento, String tipo, String dataId) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    // O lock serializa entregas simultâneas: quem chega depois enxerga o efeito do primeiro
                    MpWebhookEvento atual = eventoRepository.findParaProcessamento(evento.getId()).orElse(evento);
                    if (Boolean.TRUE.equals(atual.getProcessado())) {
                        return;
                    }
                    atual.registrarProcessamento(despachar(tipo, dataId));
                    eventoRepository.save(atual);
                });
                return;

            } catch (OptimisticLockingFailureException e) {
                if (tentativa >= MAX_TENTATIVAS_CONFLITO_VERSAO) {
                    throw e;
                }
                log.warn("Webhook {} data.id={}: assinatura alterada concorrentemente (tentativa {}/{}), reaplicando",
                        tipo, dataId, tentativa, MAX_TENTATIVAS_CONFLITO_VERSAO);
            }
        }
    }

    /**
     * Grava o erro com UPDATE condicional (só se o evento seguir aberto): salvar a cópia
     * lida antes do processamento poderia reabrir um evento que uma entrega concorrente
     * já encerrou. Agenda a próxima tentativa com backoff exponencial.
     */
    private void registrarFalha(MpWebhookEvento evento, Exception erro) {
        String mensagem = erro.getMessage() != null ? erro.getMessage() : erro.getClass().getSimpleName();
        mensagem = mensagem.substring(0, Math.min(mensagem.length(), TAMANHO_MAXIMO_ERRO));
        int tentativasAnteriores = evento.getTentativas() != null ? evento.getTentativas() : 0;
        LocalDateTime proximaTentativa = LocalDateTime.now().plus(backoff(tentativasAnteriores));
        String erroGravado = mensagem;
        try {
            transactionTemplate.executeWithoutResult(status ->
                    eventoRepository.registrarFalha(evento.getId(), erroGravado, proximaTentativa));
        } catch (Exception e) {
            log.error("Não foi possível registrar a falha do evento {}: {}", evento.getId(), e.getMessage(), e);
        }
    }

    /** 5 min, 10 min, 20 min... limitado a 6 h. */
    private static Duration backoff(int tentativasAnteriores) {
        Duration espera = BACKOFF_BASE.multipliedBy(1L << Math.min(tentativasAnteriores, 10));
        return espera.compareTo(BACKOFF_MAXIMO) > 0 ? BACKOFF_MAXIMO : espera;
    }

    private boolean faturaRegistradaComoPaga(String authorizedPaymentId) {
        return cobrancaTenantRepository.findByMpAuthorizedPaymentId(authorizedPaymentId)
                .map(CobrancaTenant::isPago)
                .orElse(false);
    }

    /**
     * @return true quando o recurso atingiu estado final e o evento pode ser encerrado
     */
    private boolean despachar(String tipo, String dataId) {
        return switch (tipo) {
            case TOPICO_PREAPPROVAL -> processarPreapproval(dataId);
            case TOPICO_AUTHORIZED_PAYMENT -> processarPagamentoAutorizado(dataId);
            default -> {
                log.info("Tópico não tratado pelo Saúde Conecta: {}", tipo);
                yield true;
            }
        };
    }

    /**
     * Persiste o evento se ele ainda não existir; reaproveita o existente enquanto
     * ele não estiver encerrado.
     *
     * @return o evento a processar, ou null quando já está encerrado
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
     * Só pausa e cancelamento alteram o status local (ver
     * {@link AssinaturaMercadoPagoService#aplicarStatusRemoto}); {@code authorized}
     * apenas atualiza o espelho do MP, sem ativar nem renovar.
     *
     * @return true quando o preapproval está cancelado (estado final)
     */
    private boolean processarPreapproval(String preapprovalId) {
        PreapprovalResponse remoto = mercadoPagoClient.buscarPreapproval(preapprovalId);
        if (remoto == null) {
            log.warn("Preapproval {} não encontrado no Mercado Pago", preapprovalId);
            return false;
        }
        if (!recursoConfere(TOPICO_PREAPPROVAL, preapprovalId, remoto.id())) {
            return false;
        }

        AssinaturaTenant assinatura = localizarAssinatura(preapprovalId, remoto.externalReference());
        if (assinatura == null) {
            return false;
        }

        assinatura.setMpPreapprovalId(remoto.id());
        assinatura.setMpStatus(remoto.status());
        assinatura.setMpUltimaSincronizacao(LocalDateTime.now());
        assinaturaMercadoPagoService.aplicarStatusRemoto(assinatura, remoto.status());

        assinaturaTenantRepository.save(assinatura);
        log.info("Assinatura {} sincronizada via webhook: status local {} (MP: {})",
                assinatura.getId(), assinatura.getStatus(), remoto.status());

        return MercadoPagoClient.STATUS_CANCELLED.equals(remoto.status());
    }

    /**
     * Lança a fatura mensal cobrada pelo Mercado Pago como CobrancaTenant.
     *
     * @return true quando a fatura está paga (estado final); recusada ou sem pagamento
     *         continuam abertas porque o Mercado Pago tenta cobrar de novo
     */
    private boolean processarPagamentoAutorizado(String authorizedPaymentId) {
        AuthorizedPaymentResponse fatura = mercadoPagoClient.buscarAuthorizedPayment(authorizedPaymentId);
        if (fatura == null) {
            log.warn("Authorized payment {} não encontrado no Mercado Pago", authorizedPaymentId);
            return false;
        }
        if (!recursoConfere(TOPICO_AUTHORIZED_PAYMENT, authorizedPaymentId, fatura.id())) {
            return false;
        }

        AssinaturaTenant assinatura = localizarAssinatura(fatura.preapprovalId(), null);
        if (assinatura == null) {
            return false;
        }

        String statusPagamento = fatura.statusPagamento();
        if (statusPagamento == null) {
            log.info("Fatura {} ainda sem pagamento processado (status={})", authorizedPaymentId, fatura.status());
            return false;
        }

        CobrancaTenant cobranca = cobrancaTenantRepository
                .findByMpAuthorizedPaymentId(authorizedPaymentId)
                .orElseGet(() -> novaCobranca(assinatura, fatura, authorizedPaymentId));

        if (cobranca.isPago()) {
            log.info("Fatura {} já registrada como paga, ignorando", authorizedPaymentId);
            return true;
        }

        if (PAGAMENTO_APROVADO.equals(statusPagamento)) {
            cobranca.setStatus(StatusCobranca.PAGO);
            cobranca.setMotivoRecusa(null);
            cobranca.setDataPagamento(LocalDateTime.now());
            cobrancaTenantRepository.save(cobranca);

            // CANCELADA é terminal: o dinheiro fica registrado, mas a reativação/estorno é manual
            if (StatusAssinatura.CANCELADA.equals(assinatura.getStatus())) {
                log.warn("Pagamento recebido em assinatura cancelada — tratar manualmente (estorno ou reativação). "
                        + "Assinatura {} (org {}), fatura {}", assinatura.getId(),
                        assinatura.getOrganizacao() != null ? assinatura.getOrganizacao().getId() : null,
                        authorizedPaymentId);
                return true;
            }

            assinaturaTenantService.renovarAposPagamento(assinatura.getId());
            // Assinatura suspensa teve o preapproval pausado: sem reativar, o MP não cobra o próximo mês
            assinaturaMercadoPagoService.reativarSePausado(assinatura);
            log.info("Pagamento aprovado da assinatura {} (fatura {})", assinatura.getId(), authorizedPaymentId);
            return true;
        }

        if (PAGAMENTO_RECUSADO.equals(statusPagamento)) {
            cobranca.setStatus(StatusCobranca.RECUSADO);
            cobranca.setMotivoRecusa(fatura.motivoRecusa());
            cobrancaTenantRepository.save(cobranca);

            // Recusa não pode tirar uma assinatura de SUSPENSA/CANCELADA de volta para INADIMPLENTE
            if (assinatura.getStatus() != null && assinatura.getStatus().isAtiva()) {
                assinaturaTenantService.marcarInadimplente(assinatura.getId());
            }
            log.warn("Pagamento recusado da assinatura {} (fatura {}, motivo {})",
                    assinatura.getId(), authorizedPaymentId, fatura.motivoRecusa());
            return false;
        }

        cobrancaTenantRepository.save(cobranca);
        log.info("Fatura {} em status intermediário: {}", authorizedPaymentId, statusPagamento);
        return false;
    }

    /**
     * O tópico vem da query/corpo e não é coberto pela assinatura HMAC (que cobre só o
     * data.id). Por isso o efeito só é aplicado quando o recurso consultado no endpoint
     * do tópico informado é de fato o recurso notificado.
     */
    private boolean recursoConfere(String tipo, String dataId, String idConsultado) {
        if (dataId.equals(idConsultado)) {
            return true;
        }
        log.warn("Webhook {} data.id={} ignorado: o recurso consultado no Mercado Pago tem id {}",
                tipo, dataId, idConsultado);
        return false;
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
