package br.com.saudeConecta.integration;

import br.com.saudeConecta.presentation.dto.usuario.BloquearUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.TodosUsuariosAgrupadosResponse;
import br.com.saudeConecta.presentation.dto.usuario.UsuarioPerfilCompletoResponse;
import br.com.saudeConecta.service.UsuarioService;
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UsuarioController - Testes de integração")
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ========== GET /usuario/buscarPerfilCompleto/{id} ==========

    @Nested
    @DisplayName("GET /usuario/buscarPerfilCompleto/{id}")
    class BuscarPerfilCompleto {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 quando perfil existe")
        void deveRetornar200QuandoExiste() throws Exception {
            UsuarioPerfilCompletoResponse response = new UsuarioPerfilCompletoResponse(
                    1L, "user@email.com", "PROFISSIONAL", 1L, null, null, null, null, null);
            when(usuarioService.buscarPerfilCompleto(1L)).thenReturn(Optional.of(response));

            mockMvc.perform(get("/usuario/buscarPerfilCompleto/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando perfil não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(usuarioService.buscarPerfilCompleto(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/usuario/buscarPerfilCompleto/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/usuario/buscarPerfilCompleto/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /usuario/buscarTodosAgrupados ==========

    @Nested
    @DisplayName("GET /usuario/buscarTodosAgrupados")
    class BuscarTodosAgrupados {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 com lista de usuários agrupados")
        void deveRetornar200ComUsuarios() throws Exception {
            TodosUsuariosAgrupadosResponse response = new TodosUsuariosAgrupadosResponse(
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList()
            );
            when(usuarioService.buscarTodosAgrupados(any())).thenReturn(response);
            when(usuarioService.buscarTodosAdminOrgsSuperAdmin()).thenReturn(response);

            mockMvc.perform(get("/usuario/buscarTodosAgrupados"))
                    .andExpect(status().isOk());
        }
    }

    // ========== PUT /usuario/bloquearUsuariobyOrg ==========

    @Nested
    @DisplayName("PUT /usuario/bloquearUsuariobyOrg")
    class BloquearUsuario {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao bloquear com sucesso")
        void deveRetornar200AoBloquear() throws Exception {
            doNothing().when(usuarioService).bloquearUsuariobyOrg(any());

            BloquearUsuarioRequest request = new BloquearUsuarioRequest(20L, 10L, 0);

            mockMvc.perform(put("/usuario/bloquearUsuariobyOrg")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando usuário não encontrado")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new IllegalArgumentException("não encontrado"))
                    .when(usuarioService).bloquearUsuariobyOrg(any());

            BloquearUsuarioRequest request = new BloquearUsuarioRequest(20L, 99L, 0);

            mockMvc.perform(put("/usuario/bloquearUsuariobyOrg")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== PUT /usuario/trocarSenharUsuariobyOrg/{id} ==========

    @Nested
    @DisplayName("PUT /usuario/trocarSenharUsuariobyOrg/{id}")
    class TrocarSenha {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao trocar senha com sucesso")
        void deveRetornar200AoTrocarSenha() throws Exception {
            doNothing().when(usuarioService).trocarSenharUsuariobyOrg(eq(1L), any());

            mockMvc.perform(put("/usuario/trocarSenharUsuariobyOrg/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("senhaNova", "novaSenha123"))))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 400 quando senha nova está em branco")
        void deveRetornar400QuandoSenhaVazia() throws Exception {
            mockMvc.perform(put("/usuario/trocarSenharUsuariobyOrg/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("senhaNova", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando usuário não encontrado ao trocar senha")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new IllegalArgumentException("não encontrado"))
                    .when(usuarioService).trocarSenharUsuariobyOrg(eq(99L), any());

            mockMvc.perform(put("/usuario/trocarSenharUsuariobyOrg/99")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("senhaNova", "senha123"))))
                    .andExpect(status().isNotFound());
        }
    }
}
