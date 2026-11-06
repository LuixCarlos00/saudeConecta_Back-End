package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.consulta.ConsultaAgendamentoService;
import br.com.saudeConecta.application.consulta.ConsultaBuscaService;
import br.com.saudeConecta.application.consulta.ConsultaEstatisticaService;
import br.com.saudeConecta.application.consulta.dto.AgendarConsultaRequest;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ConsultaController - Testes de integração")
class ConsultaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultaAgendamentoService consultaAgendamentoService;

    @MockBean
    private ConsultaBuscaService consultaBuscaService;

    @MockBean
    private ConsultaEstatisticaService consultaEstatisticaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ========== POST /consultas/cadastrarConsultaByOrg ==========

    @Nested
    @DisplayName("POST /consultas/cadastrarConsultaByOrg")
    class CadastrarConsulta {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 ao cadastrar consulta válida")
        void deveRetornar200AoCadastrarConsultaValida() throws Exception {
            AgendarConsultaRequest request = new AgendarConsultaRequest(
                    1L, 1L, null,
                    LocalDateTime.now().plusDays(1),
                    30, "Observação", null, BigDecimal.TEN
            );

            Consulta consultaMock = Consulta.builder()
                    .id(100L).status(StatusConsulta.AGENDADA)
                    .duracaoMinutos(30)
                    .dataHora(LocalDateTime.now().plusDays(1))
                    .build();

            when(consultaAgendamentoService.cadastrarConsultaByOrg(any())).thenReturn(consultaMock);

            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void deveRetornar403QuandoNaoAutenticado() throws Exception {
            AgendarConsultaRequest request = new AgendarConsultaRequest(
                    1L, 1L, null,
                    LocalDateTime.now().plusDays(1),
                    30, null, null, null
            );

            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /consultas/hoje ==========

    @Nested
    @DisplayName("GET /consultas/hoje")
    class ListarConsultasHoje {

        @Test
        @DisplayName("Deve retornar 200 com lista de consultas de hoje")
        void deveRetornar200ComListaDeConsultasHoje() throws Exception {
            // O controller usa @AuthenticationPrincipal Usuario; @WithMockUser não injeta o Usuario do domínio.
            br.com.saudeConecta.domain.usuario.Usuario usuarioLogado = new br.com.saudeConecta.domain.usuario.Usuario();
            usuarioLogado.setId(1L);
            when(consultaBuscaService.buscarConsultasHoje(any(), any(br.com.saudeConecta.domain.usuario.Usuario.class))).thenReturn(java.util.Collections.emptyList());

            mockMvc.perform(get("/consultas/hoje").with(user(usuarioLogado)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/consultas/hoje"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /consultas/horarios-ocupados-detalhado ==========

    @Nested
    @DisplayName("GET /consultas/horarios-ocupados-detalhado")
    class HorariosOcupadosDetalhado {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 com início e duração de cada horário ocupado")
        void deveRetornarInicioEDuracao() throws Exception {
            when(consultaAgendamentoService.buscarHorariosOcupadosDetalhado(5L, "2030-05-10"))
                    .thenReturn(java.util.List.of(
                            new br.com.saudeConecta.application.consulta.dto.HorarioOcupadoResponse("09:00:00", 60)));

            mockMvc.perform(get("/consultas/horarios-ocupados-detalhado")
                            .param("medicoId", "5").param("data", "2030-05-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].inicio").value("09:00:00"))
                    .andExpect(jsonPath("$[0].duracaoMinutos").value(60));
        }

        @Test
        @DisplayName("Deve retornar 403 sem autenticação")
        void deveRetornar403SemAutenticacao() throws Exception {
            mockMvc.perform(get("/consultas/horarios-ocupados-detalhado")
                            .param("medicoId", "5").param("data", "2030-05-10"))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== GET /consultas/{id} ==========

    @Nested
    @DisplayName("GET /consultas/{id}")
    class BuscarConsultaPorId {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 200 quando consulta for encontrada")
        void deveRetornar200QuandoConsultaEncontrada() throws Exception {
            br.com.saudeConecta.domain.usuario.Usuario dono = new br.com.saudeConecta.domain.usuario.Usuario();
            dono.setId(7L);
            br.com.saudeConecta.domain.profissional.Profissional profissional =
                    new br.com.saudeConecta.domain.profissional.Profissional();
            profissional.setId(3L);
            profissional.setUsuario(dono);
            Consulta consultaMock = Consulta.builder()
                    .id(1L).status(StatusConsulta.AGENDADA)
                    .duracaoMinutos(30)
                    .dataHora(LocalDateTime.now().plusDays(1))
                    .profissional(profissional)
                    .build();

            when(consultaBuscaService.buscarPorIdComRelacoes(1L)).thenReturn(java.util.Optional.of(consultaMock));

            mockMvc.perform(get("/consultas/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profissionalId").value(3))
                    .andExpect(jsonPath("$.profissionalUsuarioId").value(7));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 quando consulta não existir")
        void deveRetornar404QuandoConsultaNaoExistir() throws Exception {
            when(consultaBuscaService.buscarPorIdComRelacoes(999L)).thenReturn(java.util.Optional.empty());

            mockMvc.perform(get("/consultas/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== Isolamento multi-tenant (dashboards e buscas por organização) ==========

    @Nested
    @DisplayName("Isolamento entre clínicas")
    class IsolamentoEntreClinicas {

        @AfterEach
        void limparTenant() {
            br.com.saudeConecta.config.tenant.TenantContext.clear();
        }

        private void tenant(Long orgId, Long usuarioId) {
            br.com.saudeConecta.config.tenant.TenantContext.setCurrentTenant(orgId);
            br.com.saudeConecta.config.tenant.TenantContext.setCurrentUser(usuarioId);
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Endpoints com {organizacaoId} de outra clínica retornam 403 sem chamar o service")
        void organizacaoDaUrlDeOutraClinica403() throws Exception {
            tenant(1L, 5L);
            String[] urls = {
                    "/consultas/estatisticas/organizacao/2/dashboard",
                    "/consultas/organizacao/2/intervalo?dataInicial=2026-01-01&dataFinal=2026-01-31"
            };
            for (String url : urls) {
                tenant(1L, 5L);
                mockMvc.perform(get(url)).andExpect(status().isForbidden());
            }
            verifyNoInteractions(consultaAgendamentoService, consultaBuscaService, consultaEstatisticaService);
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Dashboard da própria clínica continua 200")
        void dashboardPropriaClinica200() throws Exception {
            tenant(1L, 5L);
            when(consultaEstatisticaService.getEstatisticasDashboardAdminOrg(1L)).thenReturn(null);

            mockMvc.perform(get("/consultas/estatisticas/organizacao/1/dashboard"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Dashboard do profissional com usuarioId de outro usuário retorna 403")
        void dashboardProfissionalOutroUsuario403() throws Exception {
            tenant(1L, 5L);

            mockMvc.perform(get("/consultas/estatisticas/dashboard/profissional").param("usuarioId", "99"))
                    .andExpect(status().isForbidden());
            verify(consultaEstatisticaService, never()).getEstatisticasDashboardProfissional(anyLong());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("Dashboard do profissional com o próprio usuarioId retorna 200")
        void dashboardProfissionalProprio200() throws Exception {
            tenant(1L, 5L);
            when(consultaEstatisticaService.getEstatisticasDashboardProfissional(5L)).thenReturn(null);

            mockMvc.perform(get("/consultas/estatisticas/dashboard/profissional").param("usuarioId", "5"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = {"ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Dashboard global é exclusivo do SUPER_ADMIN (GESTOR recebe 403)")
        void dashboardSuperAdminGestor403() throws Exception {
            mockMvc.perform(get("/consultas/estatisticas/dashboard/super-admin"))
                    .andExpect(status().isForbidden());
            verify(consultaEstatisticaService, never()).getEstatisticasDashboardSuperAdmin();
        }

        @Test
        @WithMockUser(roles = {"SUPER_ADMIN", "ADMIN", "PROFISSIONAL", "RECEPCIONISTA"})
        @DisplayName("Dashboard global liberado para o SUPER_ADMIN")
        void dashboardSuperAdminRoot200() throws Exception {
            when(consultaEstatisticaService.getEstatisticasDashboardSuperAdmin()).thenReturn(null);

            mockMvc.perform(get("/consultas/estatisticas/dashboard/super-admin"))
                    .andExpect(status().isOk());
        }
    }
}
