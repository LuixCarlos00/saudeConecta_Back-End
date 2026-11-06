package br.com.saudeConecta.integration;

import org.junit.jupiter.api.AfterEach;
import br.com.saudeConecta.application.profissional.ProfissionalService;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProfissionalController - Testes de integração")
class ProfissionalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfissionalService profissionalService;

    private ObjectMapper objectMapper;
    private Profissional profissionalMock;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Organizacao org = Organizacao.builder().nome("Clínica Teste").build();
        org.setId(1L);

        profissionalMock = Profissional.builder()
                .nome("Dr. Teste").cpf("12345678900").email("dr@teste.com")
                .registroConselho("CRM-12345").status(StatusProfissional.ATIVO)
                .organizacao(org)
                .build();
        profissionalMock.setId(10L);
    }

    // ========== GET /profissionais/buscarClinicoIdByOrg/{id} ==========

    @Nested
    @DisplayName("GET /profissionais/buscarClinicoIdByOrg/{id}")
    class BuscarClinicoIdByOrg {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 e profissional quando encontrado")
        void deveRetornar200QuandoEncontrado() throws Exception {
            when(profissionalService.buscarClinicoIdByOrg(10L)).thenReturn(Optional.of(profissionalMock));

            mockMvc.perform(get("/profissionais/buscarClinicoIdByOrg/10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 quando profissional não existir")
        void deveRetornar404QuandoNaoExistir() throws Exception {
            when(profissionalService.buscarClinicoIdByOrg(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/profissionais/buscarClinicoIdByOrg/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/profissionais/buscarClinicoIdByOrg/10"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== DELETE /profissionais/deletarClinicoIdByOrg/{id} ==========

    @Nested
    @DisplayName("DELETE /profissionais/deletarClinicoIdByOrg/{id}")
    class DeletarClinicoIdByOrg {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 ao deletar profissional com sucesso")
        void deveRetornar200AoDeletarComSucesso() throws Exception {
            doNothing().when(profissionalService).deletarClinicoIdByOrg(10L);

            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/10")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 quando profissional não for encontrado")
        void deveRetornar404QuandoNaoEncontrado() throws Exception {
            doThrow(new IllegalArgumentException("nao encontrado"))
                    .when(profissionalService).deletarClinicoIdByOrg(999L);

            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/999")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 409 quando houver relacionamentos existentes")
        void deveRetornar409QuandoHouverRelacionamentos() throws Exception {
            doThrow(new IllegalStateException("relacionamentos existentes"))
                    .when(profissionalService).deletarClinicoIdByOrg(10L);

            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/10")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    // ========== GET /profissionais/organizacao/{organizacaoId} ==========

    @Nested
    @DisplayName("GET /profissionais/organizacao/{organizacaoId}")
    class ListarPorOrganizacao {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 com lista de profissionais da organização")
        void deveRetornar200ComListaDeProfissionaisDaOrg() throws Exception {
            TenantContext.setCurrentTenant(1L);
            when(profissionalService.buscarPorOrganizacao(1L)).thenReturn(List.of(profissionalMock));

            mockMvc.perform(get("/profissionais/organizacao/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 com lista vazia quando não houver profissionais")
        void deveRetornar200ComListaVazia() throws Exception {
            TenantContext.setCurrentTenant(99L);
            when(profissionalService.buscarPorOrganizacao(99L)).thenReturn(List.of());

            mockMvc.perform(get("/profissionais/organizacao/99"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    // ========== Isolamento multi-tenant ==========

    @Nested
    @DisplayName("Isolamento entre clínicas")
    class IsolamentoEntreClinicas {

        @AfterEach
        void limparTenant() {
            TenantContext.clear();
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("GET /profissionais/organizacao/{outraOrg} retorna 403 e não lista CPF/RG/e-mail de outra clínica")
        void listarOutraClinica403() throws Exception {
            TenantContext.setCurrentTenant(1L);

            mockMvc.perform(get("/profissionais/organizacao/2"))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).buscarPorOrganizacao(anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("GET /profissionais/estatisticas/organizacao/{outraOrg}/medicos-ativos retorna 403")
        void contarOutraClinica403() throws Exception {
            TenantContext.setCurrentTenant(1L);

            mockMvc.perform(get("/profissionais/estatisticas/organizacao/2/medicos-ativos"))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).contarAtivosPorOrganizacao(anyLong());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("GET /profissionais/estatisticas/organizacao/{propriaOrg}/medicos-ativos retorna 200")
        void contarPropriaClinica200() throws Exception {
            TenantContext.setCurrentTenant(1L);
            when(profissionalService.contarAtivosPorOrganizacao(1L)).thenReturn(3L);

            mockMvc.perform(get("/profissionais/estatisticas/organizacao/1/medicos-ativos"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Estatística global de médicos ativos é exclusiva do SUPER_ADMIN (GESTOR recebe 403)")
        void estatisticaGlobalGestor403() throws Exception {
            mockMvc.perform(get("/profissionais/estatisticas/medicos-ativos"))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).getEstatisticasMedicosAtivosByOrg();
        }

        @Test
        @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Estatística global de médicos ativos liberada para o SUPER_ADMIN")
        void estatisticaGlobalRoot200() throws Exception {
            when(profissionalService.getEstatisticasMedicosAtivosByOrg()).thenReturn(10L);

            mockMvc.perform(get("/profissionais/estatisticas/medicos-ativos"))
                    .andExpect(status().isOk());
        }
    }

    // ========== 4.5d - e-mail duplicado na atualização ==========

    @Nested
    @DisplayName("PUT /profissionais/atualizarClinicoIdByOrg/{id} - e-mail duplicado")
    class AtualizarClinicoEmailDuplicado {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("4.5d - e-mail de outro usuário responde 409 com a mensagem")
        void emailDuplicadoResponde409() throws Exception {
            when(profissionalService.atualizarClinicoIdByOrg(eq(10L), any()))
                    .thenThrow(new IllegalStateException("Email ja cadastrado no sistema para outro usuario"));

            mockMvc.perform(put("/profissionais/atualizarClinicoIdByOrg/10")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Dr X\",\"cpf\":\"12345678900\",\"registroConselho\":\"CRM-1\",\"email\":\"outro@email.com\",\"sexo\":\"MASCULINO\",\"dataNascimento\":\"1985-03-15\"}"))
                    .andExpect(status().isConflict());
        }
    }

    // ========== Item 2 - rotas de gestão exigem ROLE_ADMIN ==========

    @Nested
    @DisplayName("Rotas de gestão de clínico - papel")
    class GestaoClinicoPorPapel {

        private static final String CORPO_CADASTRO = "{\"nome\":\"Dr X\",\"registroConselho\":\"CRM-1\","
                + "\"cpf\":\"12345678900\",\"email\":\"drx@clinica.com\"}";
        private static final String CORPO_ATUALIZACAO = "{\"nome\":\"Dr X\",\"cpf\":\"12345678900\","
                + "\"registroConselho\":\"CRM-1\",\"email\":\"drx@clinica.com\",\"sexo\":\"MASCULINO\","
                + "\"dataNascimento\":\"1985-03-15\"}";

        @Test
        @WithMockUser(roles = "RECEPCIONISTA")
        @DisplayName("ASSISTENTE não cadastra clínico (403)")
        void assistenteNaoCadastra() throws Exception {
            mockMvc.perform(post("/profissionais/cadastraClinicoByOrg").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).cadastraClinicoByOrg(any());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("CLINICO não cadastra clínico (403)")
        void clinicoNaoCadastra() throws Exception {
            mockMvc.perform(post("/profissionais/cadastraClinicoByOrg").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).cadastraClinicoByOrg(any());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("GESTOR cadastra clínico (201)")
        void gestorCadastra() throws Exception {
            when(profissionalService.cadastraClinicoByOrg(any())).thenReturn(profissionalMock);

            mockMvc.perform(post("/profissionais/cadastraClinicoByOrg").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_CADASTRO))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("CLINICO não deleta clínico (403)")
        void clinicoNaoDeleta() throws Exception {
            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/10").with(csrf()))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).deletarClinicoIdByOrg(anyLong());
        }

        @Test
        @WithMockUser(roles = "RECEPCIONISTA")
        @DisplayName("ASSISTENTE não deleta clínico (403)")
        void assistenteNaoDeleta() throws Exception {
            mockMvc.perform(delete("/profissionais/deletarClinicoIdByOrg/10").with(csrf()))
                    .andExpect(status().isForbidden());
            verify(profissionalService, never()).deletarClinicoIdByOrg(anyLong());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("CLINICO edita o próprio cadastro (Meus Dados): a rota não bloqueia por papel")
        void clinicoEditaASiMesmo() throws Exception {
            when(profissionalService.atualizarClinicoIdByOrg(eq(10L), any())).thenReturn(profissionalMock);

            mockMvc.perform(put("/profissionais/atualizarClinicoIdByOrg/10").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_ATUALIZACAO))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "RECEPCIONISTA")
        @DisplayName("ASSISTENTE editando o cadastro de um clínico recebe 403")
        void assistenteEditandoClinicoRecebe403() throws Exception {
            when(profissionalService.atualizarClinicoIdByOrg(eq(10L), any()))
                    .thenThrow(new br.com.saudeConecta.config.tenant.TenantAccessDeniedException("negado"));

            mockMvc.perform(put("/profissionais/atualizarClinicoIdByOrg/10").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_ATUALIZACAO))
                    .andExpect(status().isForbidden());
        }
    }
}
