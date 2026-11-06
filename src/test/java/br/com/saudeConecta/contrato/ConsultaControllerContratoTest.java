package br.com.saudeConecta.contrato;

import br.com.saudeConecta.application.consulta.ConsultaAgendamentoService;
import br.com.saudeConecta.application.consulta.ConsultaBuscaService;
import br.com.saudeConecta.application.consulta.ConsultaEstatisticaService;
import br.com.saudeConecta.application.consulta.dto.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.application.consulta.dto.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.application.dashboard.SaldoFinanceiroService;
import br.com.saudeConecta.application.dashboard.dto.SaldoFinanceiroResponse;
import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteAdminUseCase;
import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteDentistaUseCase;
import br.com.saudeConecta.application.historico.BuscarHistoricoCompletoPacienteMedicoUseCase;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de {@code /consultas}: para cada rota, o status e o formato do JSON devolvido.
 *
 * <p>Rede de segurança da fase B6, escrita contra o controller e o service antes da quebra em
 * {@code ConsultaController}/{@code ConsultaEstatisticaController}. As URLs, os status e os campos
 * do JSON verificados aqui não podem mudar com a refatoração; só os beans simulados acompanham a
 * classe que passou a responder.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Contrato HTTP - /consultas (status e JSON de cada rota)")
class ConsultaControllerContratoTest {

    private static final Long ORG_ID = 1L;
    private static final Long USUARIO_ID = 5L;

    @Autowired private MockMvc mockMvc;

    @MockBean private ConsultaAgendamentoService consultaAgendamentoService;
    @MockBean private ConsultaBuscaService consultaBuscaService;
    @MockBean private ConsultaEstatisticaService consultaEstatisticaService;
    @MockBean private SaldoFinanceiroService saldoFinanceiroService;
    @MockBean private BuscarHistoricoCompletoPacienteDentistaUseCase historicoDentista;
    @MockBean private BuscarHistoricoCompletoPacienteMedicoUseCase historicoMedico;
    @MockBean private BuscarHistoricoCompletoPacienteAdminUseCase historicoAdmin;
    @MockBean private ProfissionalRepository profissionalRepository;

    private Consulta consulta;
    private Usuario gestor;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(ORG_ID);
        TenantContext.setCurrentUser(USUARIO_ID);

        Usuario dono = new Usuario();
        dono.setId(7L);
        TipoProfissional tipo = TipoProfissional.builder().id(2L).codigo("MEDICO").build();
        Profissional profissional = new Profissional();
        profissional.setId(3L);
        profissional.setNome("Dr. Bruno");
        profissional.setRegistroConselho("CRM-123");
        profissional.setUsuario(dono);
        profissional.setTipoProfissional(tipo);
        Paciente paciente = new Paciente();
        paciente.setPaciCodigo(20L);
        paciente.setPaciNome("Maria");
        paciente.setPaciTelefone("11999990000");

        consulta = Consulta.builder()
                .id(100L)
                .profissional(profissional)
                .paciente(paciente)
                .especialidade(Especialidade.builder().id(4L).nome("Cardiologia").build())
                .formaPagamento(FormaPagamento.builder().id(6L).nome("PIX").build())
                .dataHora(LocalDateTime.of(2030, 5, 10, 9, 0))
                .duracaoMinutos(45)
                .observacoes("Retorno")
                .valor(new BigDecimal("150.00"))
                .status(StatusConsulta.AGENDADA)
                .build();

        gestor = Usuario.builder().id(USUARIO_ID).login("gestor")
                .tipoUsuarioNovo(TipoUsuarioNovo.GESTOR).status(StatusUsuario.ATIVO).build();
    }

    @AfterEach
    void limparTenant() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("Listas do gerenciamento e da agenda")
    class Listas {

        @Test
        @DisplayName("GET /hoje: 200 com todos os campos do ConsultaResponse")
        void hojeCamposCompletos() throws Exception {
            when(consultaBuscaService.buscarConsultasHoje(any(), any(Usuario.class))).thenReturn(List.of(consulta));

            mockMvc.perform(get("/consultas/hoje").param("data", "2030-05-10").with(user(gestor)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].profissionalId").value(3))
                    .andExpect(jsonPath("$[0].profissionalNome").value("Dr. Bruno"))
                    .andExpect(jsonPath("$[0].profissionalConselho").value("CRM-123"))
                    .andExpect(jsonPath("$[0].profissionalUsuarioId").value(7))
                    .andExpect(jsonPath("$[0].tipoProfissionalId").value(2))
                    .andExpect(jsonPath("$[0].tipoProfissionalNome").value("MEDICO"))
                    .andExpect(jsonPath("$[0].pacienteId").value(20))
                    .andExpect(jsonPath("$[0].pacienteNome").value("Maria"))
                    .andExpect(jsonPath("$[0].pacienteTelefone").value("11999990000"))
                    .andExpect(jsonPath("$[0].especialidadeId").value(4))
                    .andExpect(jsonPath("$[0].especialidadeNome").value("Cardiologia"))
                    .andExpect(jsonPath("$[0].dataHora").value("2030-05-10T09:00:00"))
                    .andExpect(jsonPath("$[0].dataHoraFim").value("2030-05-10T09:45:00"))
                    .andExpect(jsonPath("$[0].duracaoMinutos").value(45))
                    .andExpect(jsonPath("$[0].observacoes").value("Retorno"))
                    .andExpect(jsonPath("$[0].formaPagamentoId").value(6))
                    .andExpect(jsonPath("$[0].formaPagamentoNome").value("PIX"))
                    .andExpect(jsonPath("$[0].valor").value(150.00))
                    .andExpect(jsonPath("$[0].status").value("AGENDADA"));
            verify(consultaBuscaService).buscarConsultasHoje(eq(LocalDate.of(2030, 5, 10)), any(Usuario.class));
        }

        @Test
        @DisplayName("GET /semana-atual e /mes-atual: 200 com a lista do usuário logado")
        void semanaEMes() throws Exception {
            when(consultaBuscaService.buscarConsultasDaSemanaAtual(any(), any(Usuario.class))).thenReturn(List.of(consulta));
            when(consultaBuscaService.buscarConsultasDoMesAtual(any(), any(Usuario.class))).thenReturn(List.of());

            mockMvc.perform(get("/consultas/semana-atual").with(user(gestor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100));
            mockMvc.perform(get("/consultas/mes-atual").with(user(gestor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /ano-atual: 200 com a lista")
        void anoAtual() throws Exception {
            when(consultaBuscaService.buscarConsultasDoAnoAtual()).thenReturn(List.of(consulta));

            mockMvc.perform(get("/consultas/ano-atual"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("AGENDADA"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /buscar: filtros convertidos (datas para início/fim do dia, status separados por vírgula)")
        void buscarComFiltros() throws Exception {
            when(consultaBuscaService.buscarComFiltrosDinamicos(any(), any(), any(), any(), any())).thenReturn(List.of(consulta));

            mockMvc.perform(get("/consultas/buscar")
                            .param("profissionalId", "3").param("especialidade", "Cardio")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31")
                            .param("status", "AGENDADA, CONFIRMADA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100));
            verify(consultaBuscaService).buscarComFiltrosDinamicos(3L, "Cardio",
                    LocalDateTime.of(2030, 5, 1, 0, 0),
                    LocalDateTime.of(2030, 5, 31, 23, 59, 59),
                    List.of(StatusConsulta.AGENDADA, StatusConsulta.CONFIRMADA));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /buscar sem filtros: 200 e o service recebe tudo nulo")
        void buscarSemFiltros() throws Exception {
            when(consultaBuscaService.buscarComFiltrosDinamicos(isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/consultas/buscar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /intervalo e /profissional/{id}/intervalo: 200 com a lista")
        void intervalos() throws Exception {
            when(consultaBuscaService.buscarConsultasPorIntervalo(LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 31), "ALL"))
                    .thenReturn(List.of(consulta));
            when(consultaBuscaService.pesquisarClinicasEmIntervaloDeDatas(7L, LocalDate.of(2030, 5, 1),
                    LocalDate.of(2030, 5, 31), null)).thenReturn(List.of(consulta));

            mockMvc.perform(get("/consultas/intervalo")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31").param("status", "ALL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100));
            mockMvc.perform(get("/consultas/profissional/7/intervalo")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /intervalo com status inválido: 400 REQUISICAO_INVALIDA")
        void intervaloStatusInvalido() throws Exception {
            when(consultaBuscaService.buscarConsultasPorIntervalo(any(), any(), eq("XYZ")))
                    .thenThrow(new IllegalArgumentException("Status de consulta invalido: XYZ"));

            mockMvc.perform(get("/consultas/intervalo")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31").param("status", "XYZ"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                    .andExpect(jsonPath("$.message").value("Status de consulta invalido: XYZ"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /agenda-medico/{usuarioId}: tipoPeriodo padrão é 'diario'")
        void agendaMedico() throws Exception {
            when(consultaBuscaService.buscarConsultasPorMedicoEPeriodo(7L, "diario")).thenReturn(List.of(consulta));

            mockMvc.perform(get("/consultas/agenda-medico/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /organizacao/{id}/intervalo da própria clínica: 200")
        void organizacaoIntervaloPropria() throws Exception {
            when(consultaBuscaService.buscarConsultasPorOrganizacaoEIntervalo(ORG_ID, LocalDate.of(2030, 5, 1),
                    LocalDate.of(2030, 5, 31))).thenReturn(List.of(consulta));

            mockMvc.perform(get("/consultas/organizacao/1/intervalo")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(100));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /organizacao/{id}/intervalo de outra clínica: 403 TENANT_ACESSO_NEGADO")
        void organizacaoIntervaloOutra() throws Exception {
            mockMvc.perform(get("/consultas/organizacao/2/intervalo")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_ACESSO_NEGADO"));
            verifyNoInteractions(consultaAgendamentoService, consultaBuscaService, consultaEstatisticaService);
        }

        @Test
        @DisplayName("Sem autenticação: 403")
        void semAutenticacao() throws Exception {
            mockMvc.perform(get("/consultas/ano-atual")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Cadastro, edição, status e exclusão")
    class Escrita {

        private static final String CORPO_VALIDO = "{\"profissionalId\":3,\"pacienteId\":20,"
                + "\"dataHora\":\"2030-05-10T09:00:00\",\"duracaoMinutos\":45}";

        @Test
        @WithMockUser
        @DisplayName("POST /cadastrarConsultaByOrg: 200 com o ConsultaResponse")
        void cadastrar() throws Exception {
            when(consultaAgendamentoService.cadastrarConsultaByOrg(any())).thenReturn(consulta);

            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.dataHoraFim").value("2030-05-10T09:45:00"));
        }

        @Test
        @WithMockUser
        @DisplayName("POST /cadastrarConsultaByOrg sem campos obrigatórios: 400 VALIDACAO com erros por campo")
        void cadastrarInvalido() throws Exception {
            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDACAO"))
                    .andExpect(jsonPath("$.errors.profissionalId").exists())
                    .andExpect(jsonPath("$.errors.pacienteId").exists())
                    .andExpect(jsonPath("$.errors.dataHora").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /cadastrarConsultaByOrg com horário ocupado: 409 CONFLITO com a mensagem")
        void cadastrarHorarioOcupado() throws Exception {
            when(consultaAgendamentoService.cadastrarConsultaByOrg(any()))
                    .thenThrow(new IllegalStateException("Já existe consulta agendada para este horário"));

            mockMvc.perform(post("/consultas/cadastrarConsultaByOrg")
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLITO"))
                    .andExpect(jsonPath("$.message").value("Já existe consulta agendada para este horário"));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /atualizarConsultabyOrg/{id}: 200 com o ConsultaResponse")
        void atualizar() throws Exception {
            when(consultaAgendamentoService.atualizarConsultaByOrg(eq(100L), any())).thenReturn(consulta);

            mockMvc.perform(put("/consultas/atualizarConsultabyOrg/100")
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT /atualizarConsultabyOrg/{id} inexistente: 400 com a mensagem")
        void atualizarInexistente() throws Exception {
            when(consultaAgendamentoService.atualizarConsultaByOrg(eq(999L), any()))
                    .thenThrow(new IllegalArgumentException("Consulta não encontrada"));

            mockMvc.perform(put("/consultas/atualizarConsultabyOrg/999")
                            .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Consulta não encontrada"));
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH /{id}/status: 200 com o status novo; valor de status case-insensitive")
        void status200() throws Exception {
            consulta.setStatus(StatusConsulta.CANCELADA);
            when(consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.CANCELADA, "paciente desistiu")).thenReturn(consulta);

            mockMvc.perform(patch("/consultas/100/status").param("status", "cancelada").param("motivo", "paciente desistiu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"));
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH /{id}/status com status desconhecido: 400 sem corpo")
        void statusInvalido() throws Exception {
            mockMvc.perform(patch("/consultas/100/status").param("status", "XYZ"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(""));
            verifyNoInteractions(consultaAgendamentoService, consultaBuscaService, consultaEstatisticaService);
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH /{id}/status com transição proibida: 409 CONFLITO")
        void statusTransicaoProibida() throws Exception {
            when(consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
                    .thenThrow(new IllegalStateException("Consultas com status PAGO não podem ter o status alterado"));

            mockMvc.perform(patch("/consultas/100/status").param("status", "AGENDADA"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLITO"));
        }

        @Test
        @WithMockUser
        @DisplayName("DELETE /{id}: 204; inexistente 404 sem corpo; com prontuário 409")
        void deletar() throws Exception {
            mockMvc.perform(delete("/consultas/100")).andExpect(status().isNoContent());

            doThrow(new IllegalArgumentException("Consulta não encontrada")).when(consultaAgendamentoService).deletarConsulta(999L);
            mockMvc.perform(delete("/consultas/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(""));

            doThrow(new IllegalStateException("Não é possível excluir uma consulta que possui prontuário médico."))
                    .when(consultaAgendamentoService).deletarConsulta(101L);
            mockMvc.perform(delete("/consultas/101"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLITO"));
        }
    }

    @Nested
    @DisplayName("Agenda: horários ocupados e disponibilidade")
    class Agenda {

        @Test
        @WithMockUser
        @DisplayName("GET /horarios-ocupados: 200 com a lista de horários")
        void horariosOcupados() throws Exception {
            when(consultaAgendamentoService.buscarHorariosOcupados(3L, "2030-05-10")).thenReturn(List.of("09:00", "10:30"));

            mockMvc.perform(get("/consultas/horarios-ocupados").param("medicoId", "3").param("data", "2030-05-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("09:00"))
                    .andExpect(jsonPath("$[1]").value("10:30"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /verificarDisponibilidade: 200 com booleano puro")
        void disponibilidade() throws Exception {
            when(consultaAgendamentoService.verificarDisponibilidade("2030-05-10", "09:00", 3L)).thenReturn(true);

            mockMvc.perform(get("/consultas/verificarDisponibilidade")
                            .param("data", "2030-05-10").param("horario", "09:00").param("medicoId", "3"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /verificarDisponibilidade sem medicoId: 400")
        void disponibilidadeSemParametro() throws Exception {
            mockMvc.perform(get("/consultas/verificarDisponibilidade")
                            .param("data", "2030-05-10").param("horario", "09:00"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Estatísticas e dashboards")
    class Estatisticas {

        private final EstatisticasDashboardAdminOrgResponse dashboard = EstatisticasDashboardAdminOrgResponse.builder()
                .consultasHoje(4L).consultasAguardando(2L).consultasAtendidas(1L)
                .consultasSemana(10L).canceladosSemana(3L).confirmadosSemana(5L).build();

        @Test
        @WithMockUser
        @DisplayName("GET /estatisticas/organizacao/{id}/dashboard: 200 com os seis contadores")
        void dashboardOrganizacao() throws Exception {
            when(consultaEstatisticaService.getEstatisticasDashboardAdminOrg(ORG_ID)).thenReturn(dashboard);

            mockMvc.perform(get("/consultas/estatisticas/organizacao/1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultasHoje").value(4))
                    .andExpect(jsonPath("$.consultasAguardando").value(2))
                    .andExpect(jsonPath("$.consultasAtendidas").value(1))
                    .andExpect(jsonPath("$.consultasSemana").value(10))
                    .andExpect(jsonPath("$.canceladosSemana").value(3))
                    .andExpect(jsonPath("$.confirmadosSemana").value(5));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /estatisticas/organizacao/{id}/dashboard de outra clínica: 403 TENANT_ACESSO_NEGADO")
        void dashboardOutraOrganizacao() throws Exception {
            mockMvc.perform(get("/consultas/estatisticas/organizacao/2/dashboard"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_ACESSO_NEGADO"));
            verifyNoInteractions(consultaAgendamentoService, consultaBuscaService, consultaEstatisticaService);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /estatisticas/dashboard/profissional: próprio usuário 200; outro 403")
        void dashboardProfissional() throws Exception {
            when(consultaEstatisticaService.getEstatisticasDashboardProfissional(USUARIO_ID)).thenReturn(dashboard);

            mockMvc.perform(get("/consultas/estatisticas/dashboard/profissional").param("usuarioId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultasSemana").value(10));
            TenantContext.setCurrentTenant(ORG_ID);
            TenantContext.setCurrentUser(USUARIO_ID);
            mockMvc.perform(get("/consultas/estatisticas/dashboard/profissional").param("usuarioId", "99"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_ACESSO_NEGADO"));
            verify(consultaEstatisticaService, never()).getEstatisticasDashboardProfissional(99L);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("GET /estatisticas/dashboard/super-admin com SUPER_ADMIN: 200")
        void dashboardSuperAdmin() throws Exception {
            when(consultaEstatisticaService.getEstatisticasDashboardSuperAdmin()).thenReturn(dashboard);

            mockMvc.perform(get("/consultas/estatisticas/dashboard/super-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consultasHoje").value(4));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /estatisticas/dashboard/super-admin sem SUPER_ADMIN: 403 ACESSO_NEGADO")
        void dashboardSuperAdminNegado() throws Exception {
            mockMvc.perform(get("/consultas/estatisticas/dashboard/super-admin"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /estatisticas/medico/{id}: 200 com pares [status, quantidade] da organização do token")
        void estatisticasMedico() throws Exception {
            when(consultaEstatisticaService.buscarEstatisticasPorMedicoEIntervalo(ORG_ID, 7L,
                    LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 31)))
                    .thenReturn(List.<Object[]>of(new Object[]{"AGENDADA", 5L}, new Object[]{"REALIZADA", 3L}));

            mockMvc.perform(get("/consultas/estatisticas/medico/7")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0][0]").value("AGENDADA"))
                    .andExpect(jsonPath("$[0][1]").value(5))
                    .andExpect(jsonPath("$[1][1]").value(3));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /estatisticas/medico/{id}/duracoes: 200 com a lista de minutos")
        void duracoes() throws Exception {
            when(consultaEstatisticaService.buscarDuracoesConsultas(ORG_ID, 7L, LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 31)))
                    .thenReturn(List.of(30, 45));

            mockMvc.perform(get("/consultas/estatisticas/medico/7/duracoes")
                            .param("dataInicial", "2030-05-01").param("dataFinal", "2030-05-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value(30))
                    .andExpect(jsonPath("$[1]").value(45));
        }

        @Test
        @WithMockUser
        @DisplayName("Estatísticas por médico, durações e saldo sem organização no contexto: 400 sem corpo")
        void semOrganizacao400() throws Exception {
            String[] urls = {
                    "/consultas/estatisticas/medico/7?dataInicial=2030-05-01&dataFinal=2030-05-31",
                    "/consultas/estatisticas/medico/7/duracoes?dataInicial=2030-05-01&dataFinal=2030-05-31",
                    "/consultas/estatisticas/saldo-financeiro?inicio=2030-05-01&fim=2030-05-31"
            };
            for (String url : urls) {
                TenantContext.clear();
                mockMvc.perform(get(url))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().string(""));
            }
            verifyNoInteractions(consultaAgendamentoService, consultaBuscaService, consultaEstatisticaService, saldoFinanceiroService);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /estatisticas/saldo-financeiro: 200 com totais e detalhamento; agruparPor padrão 'mes'")
        void saldoFinanceiro() throws Exception {
            SaldoFinanceiroResponse saldo = new SaldoFinanceiroResponse(new BigDecimal("300.00"),
                    new BigDecimal("200.00"), new BigDecimal("500.00"), 2L, 1L,
                    List.of(new SaldoFinanceiroResponse.SaldoPorPeriodo("Mai/2030", new BigDecimal("300.00"),
                            new BigDecimal("200.00"), new BigDecimal("500.00"))));
            when(saldoFinanceiroService.calcularSaldo(ORG_ID, LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 31), "mes"))
                    .thenReturn(saldo);

            mockMvc.perform(get("/consultas/estatisticas/saldo-financeiro")
                            .param("inicio", "2030-05-01").param("fim", "2030-05-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalConsultas").value(300.00))
                    .andExpect(jsonPath("$.totalProcedimentos").value(200.00))
                    .andExpect(jsonPath("$.totalGeral").value(500.00))
                    .andExpect(jsonPath("$.quantidadeConsultas").value(2))
                    .andExpect(jsonPath("$.quantidadeProcedimentos").value(1))
                    .andExpect(jsonPath("$.detalhamento[0].periodo").value("Mai/2030"))
                    .andExpect(jsonPath("$.detalhamento[0].valorTotal").value(500.00));
        }
    }

    @Nested
    @DisplayName("GET /BuscandoHistoricoDeConsultasDoPaciente/{pacienteId}")
    class Historico {

        private final HistoricoConsultaPacienteResponse item = HistoricoConsultaPacienteResponse.builder()
                .tipoProntuario("MEDICO").consultaId(100L).pacienteId(20L).profissionalId(3L).build();

        private Usuario clinico() {
            return Usuario.builder().id(50L).login("medico")
                    .tipoUsuarioNovo(TipoUsuarioNovo.CLINICO).status(StatusUsuario.ATIVO).build();
        }

        @Test
        @DisplayName("Gestor: histórico completo (caso de uso admin)")
        void gestorVeTudo() throws Exception {
            when(historicoAdmin.executar(20L)).thenReturn(List.of(item));

            mockMvc.perform(get("/consultas/BuscandoHistoricoDeConsultasDoPaciente/20").with(user(gestor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tipoProntuario").value("MEDICO"))
                    .andExpect(jsonPath("$[0].consultaId").value(100))
                    .andExpect(jsonPath("$[0].pacienteId").value(20));
            verifyNoInteractions(historicoMedico, historicoDentista);
        }

        @Test
        @DisplayName("Clínico médico: só os registros dele (caso de uso médico com o id do profissional)")
        void medicoVeOsDele() throws Exception {
            Profissional medico = new Profissional();
            medico.setId(3L);
            medico.setTipoProfissional(TipoProfissional.builder().codigo("MEDICO").build());
            when(profissionalRepository.findByUsuarioIdWithRelations(50L)).thenReturn(Optional.of(medico));
            when(historicoMedico.executar(20L, 3L)).thenReturn(List.of(item));

            mockMvc.perform(get("/consultas/BuscandoHistoricoDeConsultasDoPaciente/20").with(user(clinico())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].consultaId").value(100));
            verifyNoInteractions(historicoAdmin, historicoDentista);
        }

        @Test
        @DisplayName("Clínico dentista: caso de uso do dentista")
        void dentista() throws Exception {
            Profissional dentista = new Profissional();
            dentista.setId(8L);
            dentista.setTipoProfissional(TipoProfissional.builder().codigo("DENTISTA").build());
            when(profissionalRepository.findByUsuarioIdWithRelations(50L)).thenReturn(Optional.of(dentista));
            when(historicoDentista.executar(20L, 8L)).thenReturn(List.of());

            mockMvc.perform(get("/consultas/BuscandoHistoricoDeConsultasDoPaciente/20").with(user(clinico())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
            verify(historicoDentista).executar(20L, 8L);
            verifyNoInteractions(historicoAdmin, historicoMedico);
        }

        @Test
        @DisplayName("Clínico sem cadastro de profissional: 404 sem corpo")
        void clinicoSemProfissional() throws Exception {
            when(profissionalRepository.findByUsuarioIdWithRelations(anyLong())).thenReturn(Optional.empty());

            mockMvc.perform(get("/consultas/BuscandoHistoricoDeConsultasDoPaciente/20").with(user(clinico())))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(""));
        }
    }
}
