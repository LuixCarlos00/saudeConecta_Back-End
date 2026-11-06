package br.com.saudeConecta.contrato;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de {@code GET /cep/{cep}} (proxy do ViaCEP): status e JSON de cada caminho.
 *
 * <p>Rede de segurança da fase B6, escrita antes de levar a chamada ao ViaCEP para
 * {@code infrastructure/viacep}. O ViaCEP é simulado com {@link MockRestServiceServer} preso ao
 * {@code RestTemplate} da aplicação (o mesmo bean com timeouts); a fábrica de requisições original
 * é devolvida ao fim de cada teste para não vazar para outros testes do mesmo contexto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Contrato HTTP - /cep/{cep} (proxy do ViaCEP)")
class CepControllerContratoTest {

    private static final String VIACEP = "https://viacep.com.br/ws/01310100/json";

    @Autowired private MockMvc mockMvc;
    @Autowired private RestTemplate restTemplate;

    private ClientHttpRequestFactory fabricaOriginal;
    private MockRestServiceServer viaCep;

    @BeforeEach
    void setUp() {
        fabricaOriginal = restTemplate.getRequestFactory();
        viaCep = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void restaurar() {
        restTemplate.setRequestFactory(fabricaOriginal);
    }

    @Test
    @DisplayName("CEP encontrado: 200 repassando o JSON do ViaCEP, sem autenticação")
    void cepEncontrado() throws Exception {
        viaCep.expect(requestTo(VIACEP)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"cep\":\"01310-100\",\"logradouro\":\"Avenida Paulista\","
                        + "\"bairro\":\"Bela Vista\",\"localidade\":\"São Paulo\",\"uf\":\"SP\"}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cep/01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01310-100"))
                .andExpect(jsonPath("$.logradouro").value("Avenida Paulista"))
                .andExpect(jsonPath("$.bairro").value("Bela Vista"))
                .andExpect(jsonPath("$.localidade").value("São Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"));
        viaCep.verify();
    }

    @Test
    @DisplayName("CEP formatado (com hífen) é limpo antes de chamar o ViaCEP")
    void cepFormatado() throws Exception {
        viaCep.expect(requestTo(VIACEP))
                .andRespond(withSuccess("{\"localidade\":\"São Paulo\"}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cep/01310-100")).andExpect(status().isOk());
        viaCep.verify();
    }

    @Test
    @DisplayName("CEP sem 8 dígitos: 400 {error} e o ViaCEP não é chamado")
    void cepInvalido() throws Exception {
        viaCep.expect(never(), requestTo(VIACEP));

        mockMvc.perform(get("/cep/0131010"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CEP deve conter 8 dígitos"));
        viaCep.verify();
    }

    @Test
    @DisplayName("ViaCEP responde {erro:true}: 404 sem corpo")
    void cepNaoEncontrado() throws Exception {
        viaCep.expect(requestTo(VIACEP)).andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cep/01310100"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("ViaCEP sem logradouro nem localidade: 404 sem corpo")
    void cepIncompleto() throws Exception {
        viaCep.expect(requestTo(VIACEP)).andRespond(withSuccess("{\"cep\":\"01310-100\"}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/cep/01310100"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("ViaCEP fora do ar (timeout): 502 CEP_INDISPONIVEL com o path original")
    void viaCepTimeout() throws Exception {
        viaCep.expect(requestTo(VIACEP)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        mockMvc.perform(get("/cep/01310100"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.code").value("CEP_INDISPONIVEL"))
                .andExpect(jsonPath("$.message").value("Servico de CEP indisponivel no momento. Tente novamente."))
                .andExpect(jsonPath("$.path").value("/cep/01310100"));
    }

    @Test
    @DisplayName("ViaCEP com erro 5xx: 502 CEP_INDISPONIVEL")
    void viaCepErro5xx() throws Exception {
        viaCep.expect(requestTo(VIACEP)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(get("/cep/01310100"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CEP_INDISPONIVEL"));
    }
}
