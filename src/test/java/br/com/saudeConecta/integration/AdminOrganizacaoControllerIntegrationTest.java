package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.presentation.dto.admin.AdminOrgCompletoResponse;
import br.com.saudeConecta.service.AdminOrganizacaoService;
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
@DisplayName("AdminOrganizacaoController - Testes de integração")
class AdminOrganizacaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrganizacaoService adminOrganizacaoService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // ========== GET /administrador/buscarrAdminByOrg/{id} ==========

    @Nested
    @DisplayName("GET /administrador/buscarrAdminByOrg/{id}")
    class BuscarAdminByOrg {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 quando admin existe")
        void deveRetornar200QuandoAdminExiste() throws Exception {
            AdminOrganizacao admin = new AdminOrganizacao();
            when(adminOrganizacaoService.buscarrAdminByOrg(1L)).thenReturn(Optional.of(admin));

            mockMvc.perform(get("/administrador/buscarrAdminByOrg/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando admin não existe")
        void deveRetornar404QuandoAdminNaoExiste() throws Exception {
            when(adminOrganizacaoService.buscarrAdminByOrg(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/administrador/buscarrAdminByOrg/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/administrador/buscarrAdminByOrg/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /administrador/buscarAdminOrgCompleto/{id} ==========

    @Nested
    @DisplayName("GET /administrador/buscarAdminOrgCompleto/{id}")
    class BuscarAdminOrgCompleto {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 com dados completos")
        void deveRetornar200ComDadosCompletos() throws Exception {
            AdminOrgCompletoResponse response = new AdminOrgCompletoResponse(
                    1L, "Admin", "Gerente", "admin@org.com", false, "ATIVO",
                    1L, "Clínica A", "Razão Social", "12345678000190", "CLINICA",
                    "clinica@org.com", "(11) 99999-0000",
                    1L, "01310-100", "SP", "São Paulo", "Centro", "Rua A", 100L, "");
            when(adminOrganizacaoService.buscarAdminOrgCompleto(1L)).thenReturn(response);

            mockMvc.perform(get("/administrador/buscarAdminOrgCompleto/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando admin completo não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(adminOrganizacaoService.buscarAdminOrgCompleto(99L))
                    .thenThrow(new IllegalArgumentException("Admin não encontrado"));

            mockMvc.perform(get("/administrador/buscarAdminOrgCompleto/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== DELETE /administrador/deletarAdmByOrg/{id} ==========

    @Nested
    @DisplayName("DELETE /administrador/deletarAdmByOrg/{id}")
    class DeletarAdminByOrg {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao deletar com sucesso")
        void deveRetornar200AoDeletar() throws Exception {
            doNothing().when(adminOrganizacaoService).deletarAdmByOrg(1L);

            mockMvc.perform(delete("/administrador/deletarAdmByOrg/1").with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando admin não encontrado ao deletar")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new IllegalArgumentException("não encontrado"))
                    .when(adminOrganizacaoService).deletarAdmByOrg(99L);

            mockMvc.perform(delete("/administrador/deletarAdmByOrg/99").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 409 em conflito ao deletar")
        void deveRetornar409EmConflito() throws Exception {
            doThrow(new IllegalStateException("conflito"))
                    .when(adminOrganizacaoService).deletarAdmByOrg(2L);

            mockMvc.perform(delete("/administrador/deletarAdmByOrg/2").with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    // ========== PUT /administrador/atualizarAdmByOrg/{id} ==========

    @Nested
    @DisplayName("PUT /administrador/atualizarAdmByOrg/{id}")
    class AtualizarAdminByOrg {

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 200 ao atualizar com sucesso")
        void deveRetornar200AoAtualizar() throws Exception {
            AdminOrganizacao admin = new AdminOrganizacao();
            when(adminOrganizacaoService.atualizarAdmByOrg(eq(1L), any(), any())).thenReturn(admin);

            String body = objectMapper.writeValueAsString(
                    new java.util.HashMap<String, String>() {{
                        put("nome", "Novo Nome");
                        put("email", "novo@email.com");
                    }}
            );

            mockMvc.perform(put("/administrador/atualizarAdmByOrg/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Deve retornar 404 quando admin não encontrado ao atualizar")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            when(adminOrganizacaoService.atualizarAdmByOrg(eq(99L), any(), any()))
                    .thenThrow(new IllegalArgumentException("não encontrado"));

            String body = objectMapper.writeValueAsString(
                    new java.util.HashMap<String, String>() {{
                        put("nome", "Nome");
                        put("email", "email@email.com");
                    }}
            );

            mockMvc.perform(put("/administrador/atualizarAdmByOrg/99")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }
}
