package br.com.saudeConecta.infrastructure.mercadopago;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.config.RestTemplateConfig;
import br.com.saudeConecta.infrastructure.mercadopago.dto.AuthorizedPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("MercadoPagoClient - transporte HTTP")
class MercadoPagoClientTest {

    private static final String API = "https://api.mp.teste";
    private static final String PREAPPROVAL_ID = "pa-1";

    private RestTemplate restTemplate;
    private MockRestServiceServer servidor;
    private MercadoPagoClient client;
    private MercadoPagoProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setAccessToken("token-teste");
        properties.setBackUrl("https://app/retorno");
        properties.setApiUrl(API);
        restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
        client = new MercadoPagoClient(restTemplate, properties);
    }

    private static String fatura(String id) {
        return "{\"id\":\"" + id + "\",\"preapproval_id\":\"" + PREAPPROVAL_ID
                + "\",\"status\":\"processed\",\"payment\":{\"id\":1,\"status\":\"approved\"}}";
    }

    @Test
    @DisplayName("N1 - Busca de faturas percorre todas as páginas (limit/offset) até cobrir paging.total")
    void buscaFaturasPercorreTodasAsPaginas() {
        servidor.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(API + "/authorized_payments/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("preapproval_id", PREAPPROVAL_ID))
                .andExpect(queryParam("limit", "2"))
                .andExpect(queryParam("offset", "0"))
                .andRespond(withSuccess("{\"paging\":{\"total\":3,\"limit\":2,\"offset\":0},\"results\":["
                        + fatura("f1") + "," + fatura("f2") + "]}", MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(API + "/authorized_payments/search")))
                .andExpect(queryParam("offset", "2"))
                .andRespond(withSuccess("{\"paging\":{\"total\":3,\"limit\":2,\"offset\":2},\"results\":["
                        + fatura("f3") + "]}", MediaType.APPLICATION_JSON));

        List<AuthorizedPaymentResponse> faturas = client.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID, 2);

        assertThat(faturas).extracting(AuthorizedPaymentResponse::id).containsExactly("f1", "f2", "f3");
        servidor.verify();
    }

    @Test
    @DisplayName("N1 - Sem paging na resposta: considera uma página só (sem loop)")
    void semPagingUsaUmaPaginaSo() {
        servidor.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(API + "/authorized_payments/search")))
                .andRespond(withSuccess("{\"results\":[" + fatura("f1") + "," + fatura("f2") + "]}",
                        MediaType.APPLICATION_JSON));

        List<AuthorizedPaymentResponse> faturas = client.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID, 2);

        assertThat(faturas).extracting(AuthorizedPaymentResponse::id).containsExactly("f1", "f2");
        servidor.verify();
    }

    @Test
    @DisplayName("N1 - paging.total inconsistente (páginas sempre cheias): para no teto de páginas")
    void paraNoTetoDePaginas() {
        servidor.expect(org.springframework.test.web.client.ExpectedCount.times(MercadoPagoClient.MAXIMO_PAGINAS_FATURAS),
                        requestTo(org.hamcrest.Matchers.startsWith(API + "/authorized_payments/search")))
                .andRespond(withSuccess("{\"paging\":{\"total\":999999,\"limit\":1,\"offset\":0},\"results\":["
                        + fatura("fx") + "]}", MediaType.APPLICATION_JSON));

        List<AuthorizedPaymentResponse> faturas = client.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID, 1);

        assertThat(faturas).hasSize(MercadoPagoClient.MAXIMO_PAGINAS_FATURAS);
        servidor.verify();
    }

    @Test
    @DisplayName("N1 - Página vazia encerra a busca mesmo com total maior")
    void paginaVaziaEncerra() {
        servidor.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(API + "/authorized_payments/search")))
                .andRespond(withSuccess("{\"paging\":{\"total\":10,\"limit\":2,\"offset\":0},\"results\":[]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID, 2)).isEmpty();
        servidor.verify();
    }

    @Test
    @DisplayName("N3 - Read timeout do RestTemplate configurado vira BusinessException 502 (não trava a thread)")
    void readTimeoutViraBusinessException() throws Exception {
        // Socket que aceita a conexão (backlog do SO) e nunca responde
        try (ServerSocket mudo = new ServerSocket(0)) {
            properties.setApiUrl("http://127.0.0.1:" + mudo.getLocalPort());
            RestTemplate comTimeout = new RestTemplateConfig()
                    .restTemplate(Duration.ofSeconds(2), Duration.ofMillis(300));
            MercadoPagoClient clientComTimeout = new MercadoPagoClient(comTimeout, properties);

            long inicio = System.nanoTime();
            assertThatThrownBy(() -> clientComTimeout.buscarPreapproval(PREAPPROVAL_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
            assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(5));
        }
    }

    @Test
    @DisplayName("Busca de faturas usa limit aceito pelo MP (máximo 15; acima disso responde 400)")
    void buscaFaturasUsaLimitAceitoPeloMp() {
        assertThat(MercadoPagoClient.TAMANHO_PAGINA_FATURAS).isBetween(1, 15);
        servidor.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(API + "/authorized_payments/search")))
                .andExpect(queryParam("limit", String.valueOf(MercadoPagoClient.TAMANHO_PAGINA_FATURAS)))
                .andExpect(queryParam("offset", "0"))
                .andRespond(withSuccess("{\"paging\":{\"total\":1,\"limit\":12,\"offset\":0},\"results\":["
                        + fatura("f1") + "]}", MediaType.APPLICATION_JSON));

        assertThat(client.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID)).hasSize(1);
        servidor.verify();
    }
}
