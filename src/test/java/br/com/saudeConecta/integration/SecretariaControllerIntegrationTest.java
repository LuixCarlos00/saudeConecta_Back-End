package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaRequest;
import br.com.saudeConecta.service.SecretariaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecretariaController - Testes de integração")
class SecretariaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecretariaService secretariaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private Secretaria criarSecretaria(Long id) {
        Secretaria sec = new Secretaria();
        sec.setId(id);
        sec.setNome("Ana Silva");
        sec.setEmail("ana@clinica.com");
        sec.setStatus(StatusSecretaria.ATIVO);
        return sec;
    }

    // ========== GET /secretaria/buscarSecretariaIdByOrg/{id} ==========

    @Nested
    @DisplayName("GET /secretaria/buscarSecretariaIdByOrg/{id}")
    class BuscarSecretaria {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 quando secretária existe")
        void deveRetornar200QuandoExiste() throws Exception {
            when(secretariaService.buscarSecretariaIdByOrg(1L))
                    .thenReturn(Optional.of(criarSecretaria(1L)));

            mockMvc.perform(get("/secretaria/buscarSecretariaIdByOrg/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando secretária não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(secretariaService.buscarSecretariaIdByOrg(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/secretaria/buscarSecretariaIdByOrg/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/secretaria/buscarSecretariaIdByOrg/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== POST /secretaria/cadastrarSecretariaByOrg ==========

    @Nested
    @DisplayName("POST /secretaria/cadastrarSecretariaByOrg")
    class CadastrarSecretaria {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 201 ao cadastrar com sucesso")
        void deveRetornar201AoCadastrar() throws Exception {
            Secretaria secretaria = criarSecretaria(1L);
            when(secretariaService.cadastrarSecretariaByOrg(any())).thenReturn(secretaria);

            CadastrarSecretariaRequest request = new CadastrarSecretariaRequest(
                    "Ana Silva", "11122233344", "ana@clinica.com", "(11) 99999-0000"
            );

            mockMvc.perform(post("/secretaria/cadastrarSecretariaByOrg")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 409 quando email já cadastrado")
        void deveRetornar409EmConflito() throws Exception {
            when(secretariaService.cadastrarSecretariaByOrg(any()))
                    .thenThrow(new IllegalStateException("Email já cadastrado"));

            CadastrarSecretariaRequest request = new CadastrarSecretariaRequest(
                    "Ana Silva", "11122233344", "ana@clinica.com", "(11) 99999-0000"
            );

            mockMvc.perform(post("/secretaria/cadastrarSecretariaByOrg")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ========== DELETE /secretaria/deletarSecretariaIdByOrg/{id} ==========

    @Nested
    @DisplayName("DELETE /secretaria/deletarSecretariaIdByOrg/{id}")
    class DeletarSecretaria {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao deletar com sucesso")
        void deveRetornar200AoDeletar() throws Exception {
            doNothing().when(secretariaService).deletarSecretariaIdByOrg(1L);

            mockMvc.perform(delete("/secretaria/deletarSecretariaIdByOrg/1").with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando secretária não encontrada")
        void deveRetornar404QuandoNaoEncontrada() throws Exception {
            doThrow(new IllegalArgumentException("não encontrada"))
                    .when(secretariaService).deletarSecretariaIdByOrg(99L);

            mockMvc.perform(delete("/secretaria/deletarSecretariaIdByOrg/99").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 409 em conflito ao deletar")
        void deveRetornar409EmConflito() throws Exception {
            doThrow(new IllegalStateException("conflito"))
                    .when(secretariaService).deletarSecretariaIdByOrg(2L);

            mockMvc.perform(delete("/secretaria/deletarSecretariaIdByOrg/2").with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}
