package br.com.saudeConecta.service.mercadopago;

import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.service.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.service.mercadopago.dto.AuthorizedPaymentSearchResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP da API de assinaturas do Mercado Pago.
 * Responsável apenas pelo transporte — nenhuma regra de negócio vive aqui.
 */
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private static final String PATH_PREAPPROVAL = "/preapproval";
    private static final String PATH_AUTHORIZED_PAYMENT = "/authorized_payments/";
    private static final String PATH_AUTHORIZED_PAYMENT_SEARCH = "/authorized_payments/search?preapproval_id=";
    /** Tamanho de página pedido na busca de faturas. */
    static final int TAMANHO_PAGINA_FATURAS = 50;
    /** Teto de páginas na busca de faturas (50 x 20 = 1000 faturas, ~83 anos de mensalidades). */
    static final int MAXIMO_PAGINAS_FATURAS = 20;
    private static final String CURRENCY_BRL = "BRL";
    private static final String FREQUENCY_TYPE_MONTHS = "months";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_AUTHORIZED = "authorized";
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_CANCELLED = "cancelled";

    private final RestTemplate restTemplate;
    private final MercadoPagoProperties properties;

    public MercadoPagoClient(RestTemplate restTemplate, MercadoPagoProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Cria uma assinatura sem plano associado com status "pending".
     * O cliente finaliza o cadastro do cartão no checkout hospedado (init_point).
     *
     * @param reason            descrição exibida na fatura do cliente
     * @param externalReference referência local da assinatura (ASSINATURA-{id})
     * @param payerEmail        e-mail da conta Mercado Pago do pagador
     * @param valorMensal       valor da recorrência mensal
     * @return preapproval criado, contendo id e init_point
     */
    public PreapprovalResponse criarPreapprovalPendente(String reason, String externalReference,
                                                        String payerEmail, BigDecimal valorMensal) {
        Map<String, Object> corpo = Map.of(
                "reason", reason,
                "external_reference", externalReference,
                "payer_email", payerEmail,
                "back_url", properties.getBackUrl(),
                "status", STATUS_PENDING,
                "auto_recurring", Map.of(
                        "frequency", 1,
                        "frequency_type", FREQUENCY_TYPE_MONTHS,
                        "transaction_amount", valorMensal,
                        "currency_id", CURRENCY_BRL
                )
        );

        return executar(HttpMethod.POST, PATH_PREAPPROVAL, corpo, PreapprovalResponse.class,
                "criar preapproval para " + externalReference);
    }

    /**
     * Consulta o estado atual de uma assinatura no Mercado Pago.
     *
     * @param preapprovalId identificador do preapproval
     * @return dados atualizados da assinatura
     */
    public PreapprovalResponse buscarPreapproval(String preapprovalId) {
        return executar(HttpMethod.GET, PATH_PREAPPROVAL + "/" + preapprovalId, null,
                PreapprovalResponse.class, "buscar preapproval " + preapprovalId);
    }

    /**
     * Atualiza o valor cobrado mensalmente (usado em troca de plano).
     *
     * @param preapprovalId identificador do preapproval
     * @param novoValor     novo valor mensal
     * @return assinatura atualizada
     */
    public PreapprovalResponse atualizarValor(String preapprovalId, BigDecimal novoValor) {
        Map<String, Object> corpo = Map.of("auto_recurring", Map.of(
                "transaction_amount", novoValor,
                "currency_id", CURRENCY_BRL
        ));
        return executar(HttpMethod.PUT, PATH_PREAPPROVAL + "/" + preapprovalId, corpo,
                PreapprovalResponse.class, "atualizar valor do preapproval " + preapprovalId);
    }

    /**
     * Cancela definitivamente a assinatura no Mercado Pago.
     *
     * @param preapprovalId identificador do preapproval
     * @return assinatura com status cancelled
     */
    public PreapprovalResponse cancelarPreapproval(String preapprovalId) {
        return alterarStatus(preapprovalId, STATUS_CANCELLED);
    }

    /**
     * Pausa a assinatura (usado quando o tenant é suspenso por inadimplência).
     *
     * @param preapprovalId identificador do preapproval
     * @return assinatura com status paused
     */
    public PreapprovalResponse pausarPreapproval(String preapprovalId) {
        return alterarStatus(preapprovalId, STATUS_PAUSED);
    }

    /**
     * Consulta uma fatura mensal gerada pela assinatura.
     *
     * @param authorizedPaymentId identificador do pagamento autorizado
     * @return dados da fatura e do pagamento processado
     */
    public AuthorizedPaymentResponse buscarAuthorizedPayment(String authorizedPaymentId) {
        return executar(HttpMethod.GET, PATH_AUTHORIZED_PAYMENT + authorizedPaymentId, null,
                AuthorizedPaymentResponse.class, "buscar authorized payment " + authorizedPaymentId);
    }

    /**
     * Reativa uma assinatura pausada (status volta a "authorized" e o MP retoma as cobranças).
     *
     * @param preapprovalId identificador do preapproval
     * @return assinatura com status authorized
     */
    public PreapprovalResponse reativarPreapproval(String preapprovalId) {
        return alterarStatus(preapprovalId, STATUS_AUTHORIZED);
    }

    /**
     * Lista as faturas (authorized payments) geradas por uma assinatura.
     * Usado pela reconciliação para achar pagamentos aprovados cujo webhook se perdeu.
     *
     * Paginação defensiva: pede páginas de {@link #TAMANHO_PAGINA_FATURAS} via
     * {@code limit/offset} e segue até cobrir {@code paging.total}. Se o MP não devolver
     * {@code paging}, considera uma página só (com WARN); o teto de
     * {@link #MAXIMO_PAGINAS_FATURAS} páginas impede loop se o total vier inconsistente.
     * Não pede ordenação: o parâmetro de ordenação deste endpoint não é documentado e um
     * parâmetro recusado derrubaria a recuperação; percorrer todas as páginas já cobre
     * a fatura mais recente.
     *
     * ❓ Contrato a validar no sandbox do Mercado Pago (aceite de limit/offset e formato de paging).
     *
     * @param preapprovalId identificador do preapproval
     * @return faturas da assinatura (lista vazia quando não há nenhuma)
     */
    public List<AuthorizedPaymentResponse> buscarAuthorizedPaymentsDoPreapproval(String preapprovalId) {
        return buscarAuthorizedPaymentsDoPreapproval(preapprovalId, TAMANHO_PAGINA_FATURAS);
    }

    /** Variante com tamanho de página configurável (testes). */
    List<AuthorizedPaymentResponse> buscarAuthorizedPaymentsDoPreapproval(String preapprovalId, int tamanhoPagina) {
        List<AuthorizedPaymentResponse> faturas = new ArrayList<>();
        int offset = 0;
        for (int pagina = 1; pagina <= MAXIMO_PAGINAS_FATURAS; pagina++) {
            AuthorizedPaymentSearchResponse resposta = executar(HttpMethod.GET,
                    PATH_AUTHORIZED_PAYMENT_SEARCH + preapprovalId + "&limit=" + tamanhoPagina + "&offset=" + offset,
                    null, AuthorizedPaymentSearchResponse.class,
                    "buscar faturas do preapproval " + preapprovalId + " (offset " + offset + ")");
            if (resposta == null) {
                break;
            }
            List<AuthorizedPaymentResponse> paginaAtual = resposta.faturas();
            faturas.addAll(paginaAtual);

            AuthorizedPaymentSearchResponse.Paging paging = resposta.paging();
            if (paging == null || paging.total() == null) {
                log.warn("Busca de faturas do preapproval {} sem paging: considerando só a 1ª página ({} fatura(s))",
                        preapprovalId, paginaAtual.size());
                break;
            }
            // Avança pelo que veio (o MP pode aplicar um limit menor que o pedido)
            offset += paginaAtual.size();
            if (paginaAtual.isEmpty() || offset >= paging.total()) {
                break;
            }
            if (pagina == MAXIMO_PAGINAS_FATURAS) {
                log.warn("Busca de faturas do preapproval {} parou no teto de {} páginas ({} de {} faturas lidas)",
                        preapprovalId, MAXIMO_PAGINAS_FATURAS, offset, paging.total());
            }
        }
        return faturas;
    }

    private PreapprovalResponse alterarStatus(String preapprovalId, String status) {
        return executar(HttpMethod.PUT, PATH_PREAPPROVAL + "/" + preapprovalId,
                Map.of("status", status), PreapprovalResponse.class,
                "alterar status do preapproval " + preapprovalId + " para " + status);
    }

    private <T> T executar(HttpMethod metodo, String path, Object corpo, Class<T> tipoResposta, String operacao) {
        if (!properties.isConfigurado()) {
            throw new BusinessException(
                    "Integração com o Mercado Pago não configurada. Defina MP_ACCESS_TOKEN e MP_BACK_URL.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<T> resposta = restTemplate.exchange(
                    properties.getApiUrl() + path, metodo, new HttpEntity<>(corpo, headers), tipoResposta);
            return resposta.getBody();

        } catch (RestClientException e) {
            log.error("Falha ao {} no Mercado Pago: {}", operacao, e.getMessage(), e);
            throw new BusinessException(
                    "Não foi possível comunicar com o Mercado Pago. Tente novamente em instantes.",
                    HttpStatus.BAD_GATEWAY, e);
        }
    }
}
