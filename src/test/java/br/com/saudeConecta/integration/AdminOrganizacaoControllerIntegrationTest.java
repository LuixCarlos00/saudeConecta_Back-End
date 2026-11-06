package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.organizacao.AdminOrganizacaoService;
import br.com.saudeConecta.application.organizacao.dto.AdminOrgCompletoResponse;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
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
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Deve retornar 200 com dados completos")
        void deveRetornar200ComDadosCompletos() throws Exception {
            AdminOrgCompletoResponse response = new AdminOrgCompletoResponse(
                    1L, "Admin", "Gerente", "admin@org.com", false, "ATIVO",
                    1L, "Clínica A", "Razão Social", "12345678000190", null, "CLINICA",
                    "clinica@org.com", "(11) 99999-0000",
                    1L, "01310-100", "SP", "São Paulo", "Centro", "Rua A", 100L, "");
            when(adminOrganizacaoService.buscarAdminOrgCompleto(1L)).thenReturn(response);

            mockMvc.perform(get("/administrador/buscarAdminOrgCompleto/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("Deve retornar 404 quando admin completo não existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            when(adminOrganizacaoService.buscarAdminOrgCompleto(99L))
                    .thenThrow(new IllegalArgumentException("Admin não encontrado"));

            mockMvc.perform(get("/administrador/buscarAdminOrgCompleto/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 403 para GESTOR (não é SUPER_ADMIN)")
        void deveRetornar403ParaGestor() throws Exception {
            mockMvc.perform(get("/administrador/buscarAdminOrgCompleto/1"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(adminOrganizacaoService);
        }
    }

    // ========== DELETE /administrador/deletarAdmByOrg/{id} ==========

    @Nested
    @DisplayName("DELETE /administrador/deletarAdmByOrg/{id}")
    class DeletarAdminByOrg {

        @Test
        @WithMockUser(roles = "RECEPCIONISTA")
        @DisplayName("Deve retornar 403 para ASSISTENTE (sem ROLE_ADMIN)")
        void deveRetornar403ParaAssistente() throws Exception {
            mockMvc.perform(delete("/administrador/deletarAdmByOrg/1").with(csrf()))
                    .andExpect(status().isForbidden());
            verify(adminOrganizacaoService, never()).deletarAdmByOrg(anyLong());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Deve retornar 403 para CLINICO (sem ROLE_ADMIN)")
        void deveRetornar403ParaClinico() throws Exception {
            mockMvc.perform(delete("/administrador/deletarAdmByOrg/1").with(csrf()))
                    .andExpect(status().isForbidden());
            verify(adminOrganizacaoService, never()).deletarAdmByOrg(anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 200 ao deletar com sucesso")
        void deveRetornar200AoDeletar() throws Exception {
            doNothing().when(adminOrganizacaoService).deletarAdmByOrg(1L);

            mockMvc.perform(delete("/administrador/deletarAdmByOrg/1").with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 404 quando admin não encontrado ao deletar")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new IllegalArgumentException("não encontrado"))
                    .when(adminOrganizacaoService).deletarAdmByOrg(99L);

            mockMvc.perform(delete("/administrador/deletarAdmByOrg/99").with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
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
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
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
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
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

        @Test
        @WithMockUser(roles = "RECEPCIONISTA")
        @DisplayName("Deve retornar 403 para ASSISTENTE (sem ROLE_ADMIN)")
        void deveRetornar403ParaAssistente() throws Exception {
            mockMvc.perform(put("/administrador/atualizarAdmByOrg/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"email\":\"invasor@email.com\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(adminOrganizacaoService);
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Deve retornar 403 para CLINICO (sem ROLE_ADMIN)")
        void deveRetornar403ParaClinico() throws Exception {
            mockMvc.perform(put("/administrador/atualizarAdmByOrg/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"email\":\"invasor@email.com\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(adminOrganizacaoService);
        }
    }

    // ========== PUT /administrador/atualizarAdminOrgCompleto/{id} ==========

    @Nested
    @DisplayName("PUT /administrador/atualizarAdminOrgCompleto/{id}")
    class AtualizarAdminOrgCompleto {

        @Test
        @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 200 para SUPER_ADMIN")
        void deveRetornar200ParaSuperAdmin() throws Exception {
            mockMvc.perform(put("/administrador/atualizarAdminOrgCompleto/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Novo\"}"))
                    .andExpect(status().isOk());
            verify(adminOrganizacaoService).atualizarAdminOrgCompleto(eq(1L), any());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 403 para GESTOR")
        void deveRetornar403ParaGestor() throws Exception {
            mockMvc.perform(put("/administrador/atualizarAdminOrgCompleto/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Novo\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(adminOrganizacaoService);
        }
    }

    // ========== POST /administrador/cadastrarAdminOrgCompleto ==========

    @Nested
    @DisplayName("POST /administrador/cadastrarAdminOrgCompleto")
    class CadastrarAdminOrgCompleto {

        private static final String BODY_VALIDO = """
                {"tipoPessoa":"FISICA","nome":"Gestor Novo","cpf":"12345678900","cargo":"Gestor",
                 "email":"gestor@novo.com","cep":"01310-100","uf":"SP","municipio":"São Paulo",
                 "rua":"Rua A","numero":100,"planoId":1,"cortesia":true}
                """;

        @Test
        @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 201 para SUPER_ADMIN")
        void deveRetornar201ParaSuperAdmin() throws Exception {
            when(adminOrganizacaoService.cadastrarAdminOrgCompleto(any())).thenReturn(new AdminOrganizacao());

            mockMvc.perform(post("/administrador/cadastrarAdminOrgCompleto")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY_VALIDO))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Deve retornar 403 para GESTOR (impede criar tenant com cortesia)")
        void deveRetornar403ParaGestor() throws Exception {
            mockMvc.perform(post("/administrador/cadastrarAdminOrgCompleto")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY_VALIDO))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(adminOrganizacaoService);
        }
    }

    // ========== 4.5d - e-mail duplicado na atualização ==========

    @Nested
    @DisplayName("4.5d - atualização com e-mail de outro usuário responde 409")
    class AtualizacaoEmailDuplicado {

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("PUT atualizarAdmByOrg: 409 com a mensagem (antes gravava o e-mail duplicado)")
        void atualizarAdmByOrgResponde409() throws Exception {
            when(adminOrganizacaoService.atualizarAdmByOrg(eq(1L), any(), any()))
                    .thenThrow(new IllegalStateException("Email ja cadastrado no sistema para outro usuario"));

            mockMvc.perform(put("/administrador/atualizarAdmByOrg/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"email\":\"outro@email.com\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Email ja cadastrado")));
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("PUT atualizarMeusDados: 409 com a mensagem")
        void atualizarMeusDadosResponde409() throws Exception {
            doThrow(new IllegalStateException("Email ja cadastrado no sistema para outro usuario"))
                    .when(adminOrganizacaoService).atualizarMeusDadosAdminOrg(eq(1L), any());

            mockMvc.perform(put("/administrador/atualizarMeusDados/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"X\",\"email\":\"outro@email.com\"}"))
                    .andExpect(status().isConflict());
        }
    }

    // ========== Item 2 - cadastro de gestor e "meus dados" ==========

    @Nested
    @DisplayName("POST /administrador/cadastrarAdminByOrg e PUT atualizarMeusDados - papel e dono")
    class CadastroGestorEMeusDados {

        private static final String CORPO_CADASTRO =
                "{\"nome\":\"Novo Gestor\",\"cpf\":\"12345678900\",\"email\":\"gestor@clinica.com\"}";

        @Test
        @WithMockUser(roles = "RECEPCIONISTA")
        @DisplayName("ASSISTENTE não cadastra gestor (403)")
        void assistenteNaoCadastraGestor() throws Exception {
            mockMvc.perform(post("/administrador/cadastrarAdminByOrg").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                    .andExpect(status().isForbidden());
            verify(adminOrganizacaoService, never()).cadastrarAdminByOrg(any(), anyLong());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("CLINICO não cadastra gestor (403)")
        void clinicoNaoCadastraGestor() throws Exception {
            mockMvc.perform(post("/administrador/cadastrarAdminByOrg").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                    .andExpect(status().isForbidden());
            verify(adminOrganizacaoService, never()).cadastrarAdminByOrg(any(), anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("GESTOR cadastra gestor da própria clínica (201)")
        void gestorCadastraGestor() throws Exception {
            br.com.saudeConecta.config.tenant.TenantContext.setCurrentTenant(1L);
            try {
                when(adminOrganizacaoService.cadastrarAdminByOrg(any(), eq(1L))).thenReturn(new AdminOrganizacao());

                mockMvc.perform(post("/administrador/cadastrarAdminByOrg").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                        .andExpect(status().isCreated());
            } finally {
                br.com.saudeConecta.config.tenant.TenantContext.clear();
            }
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Meus Dados de outro admin responde 403")
        void meusDadosDeOutroAdminResponde403() throws Exception {
            doThrow(new br.com.saudeConecta.config.tenant.TenantAccessDeniedException("negado"))
                    .when(adminOrganizacaoService).atualizarMeusDadosAdminOrg(eq(2L), any());

            mockMvc.perform(put("/administrador/atualizarMeusDados/2").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}
