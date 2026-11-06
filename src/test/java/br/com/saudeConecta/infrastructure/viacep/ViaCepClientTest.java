package br.com.saudeConecta.infrastructure.viacep;

import br.com.saudeConecta.application.endereco.port.CepIndisponivelException;
import br.com.saudeConecta.application.endereco.port.CepNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ViaCepClient - adaptador do ViaCEP")
class ViaCepClientTest {

    private static final String URL = "https://viacep.com.br/ws/01310100/json";

    private MockRestServiceServer servidor;
    private ViaCepClient cliente;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
        cliente = new ViaCepClient(restTemplate);
    }

    @Test
    @DisplayName("CEP encontrado: devolve o corpo do ViaCEP sem conversão")
    void encontrado() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"logradouro\":\"Avenida Paulista\",\"localidade\":\"São Paulo\"}", MediaType.APPLICATION_JSON));

        Object endereco = cliente.buscar("01310100");

        assertThat(endereco).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.map(String.class, Object.class))
                .containsEntry("logradouro", "Avenida Paulista");
        servidor.verify();
    }

    @Test
    @DisplayName("Só com localidade (CEP geral de cidade) ainda é encontrado")
    void soLocalidade() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"localidade\":\"Cidade\"}", MediaType.APPLICATION_JSON));

        assertThat(cliente.buscar("01310100")).isNotNull();
    }

    @Test
    @DisplayName("{erro:true}: CepNaoEncontradoException")
    void erroTrue() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.buscar("01310100")).isInstanceOf(CepNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Sem logradouro e sem localidade: CepNaoEncontradoException")
    void incompleto() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"cep\":\"01310-100\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.buscar("01310100")).isInstanceOf(CepNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Timeout: CepIndisponivelException com a causa original")
    void timeout() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> cliente.buscar("01310100"))
                .isInstanceOf(CepIndisponivelException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("Erro 5xx do ViaCEP: CepIndisponivelException")
    void erro5xx() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> cliente.buscar("01310100")).isInstanceOf(CepIndisponivelException.class);
    }
}
