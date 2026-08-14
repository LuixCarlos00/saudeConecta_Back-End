package br.com.saudeConecta.integration;

import br.com.saudeConecta.presentation.dto.usuario.DadosLoginUsuario;
import br.com.saudeConecta.service.HomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HomeController - Testes de integração (endpoints públicos)")
class HomeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeService homeService;

    @MockBean
    private AuthenticationManager authenticationManager;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ========== POST /Home/login ==========

    @Nested
    @DisplayName("POST /Home/login - Endpoint público")
    class Login {

        @Test
        @DisplayName("Deve retornar erro (5xx) quando credenciais inválidas disparam BadCredentialsException")
        void deveRetornarErroQuandoCredenciaisInvalidas() throws Exception {
            DadosLoginUsuario dados = new DadosLoginUsuario("cpf_errado", "senha_errada", "GESTOR");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Credenciais inválidas"));

            mockMvc.perform(post("/Home/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dados)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("Deve retornar erro de servidor quando body está ausente no login")
        void deveRetornarErroDeServidorQuandoBodyAusente() throws Exception {
            mockMvc.perform(post("/Home/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ========== POST /Home/esqueciMinhaSenha ==========

    @Nested
    @DisplayName("POST /Home/esqueciMinhaSenha - Endpoint público")
    class EsqueciMinhaSenha {

        @Test
        @DisplayName("Deve retornar 200 ao solicitar recuperação de senha para email válido")
        void deveRetornar200AoSolicitarRecuperacao() throws Exception {
            doNothing().when(homeService).recuperarSenhaPorEmail(eq("usuario@teste.com"));

            mockMvc.perform(post("/Home/esqueciMinhaSenha")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"usuario@teste.com\"}"))
                    .andExpect(status().isOk());
        }
    }

    // ========== GET /cep/{cep} ==========

    @Nested
    @DisplayName("GET /cep/{cep} - Endpoint público")
    class BuscarCep {

        @Test
        @DisplayName("Deve retornar 200 sem autenticação para endpoint de CEP")
        void devePermitirAcessoSemAutenticacaoParaCep() throws Exception {
            mockMvc.perform(get("/cep/01310100"))
                    .andExpect(status().isOk());
        }
    }
}
