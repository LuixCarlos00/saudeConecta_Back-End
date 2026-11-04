package br.com.saudeConecta.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4.8 - CORS vem de um único bean lido de app.cors.allowed-origins (antes: dois lugares, com
 * localhost liberado em produção) e as regras permitAll sem handler foram removidas.
 */
@SpringBootTest(properties = "app.cors.allowed-origins=https://front.permitido.example, https://outro.permitido.example")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Segurança - CORS configurável e rotas públicas mortas removidas")
class SegurancaCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Preflight de origem permitida (da propriedade) recebe os cabeçalhos CORS")
    void preflightOrigemPermitida() throws Exception {
        mockMvc.perform(options("/consultas")
                        .header(HttpHeaders.ORIGIN, "https://outro.permitido.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://outro.permitido.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Preflight de origem fora da lista (inclusive localhost, se não configurado) é negado")
    void preflightOrigemNegada() throws Exception {
        mockMvc.perform(options("/consultas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        mockMvc.perform(options("/consultas")
                        .header(HttpHeaders.ORIGIN, "https://atacante.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /Home/cadastralogin",
            "GET, /Home/buscarUsuarioExistente/123",
            "PUT, /Home/trocaDeSenha/1",
            "GET, /paciente/buscarPorEmail/a@a.com",
            "GET, /medico/buscarPorEmail/a@a.com",
            "GET, /paciente/InserirCodigo/123"
    })
    @DisplayName("Rotas permitAll sem handler deixam de ser públicas (anônimo recebe 403, não 404)")
    void rotasMortasExigemAutenticacao(String metodo, String uri) throws Exception {
        mockMvc.perform(request(org.springframework.http.HttpMethod.valueOf(metodo), uri))
                .andExpect(status().isForbidden());
    }
}
