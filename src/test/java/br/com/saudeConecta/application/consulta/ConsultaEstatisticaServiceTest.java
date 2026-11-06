package br.com.saudeConecta.application.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.ConsultaHistorico;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
 import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.application.consulta.dto.AgendarConsultaRequest;
import br.com.saudeConecta.application.consulta.dto.AtualizarConsultaRequest;
import br.com.saudeConecta.application.consulta.dto.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaEstatisticaService - dashboards, contagens e durações")
class ConsultaEstatisticaServiceTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private ConsultaHistoricoRepository historicoRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private EspecialidadeRepository especialidadeRepository;
    @Mock private FormaPagamentoRepository formaPagamentoRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProntuarioDentistaRepository prontuarioDentistaRepository;
    @Mock private PlanejamentoTerapeuticoRepository planejamentoTerapeuticoRepository;
    @Mock private TermoAutorizacaoRepository termoAutorizacaoRepository;
    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private TenantHelper tenantHelper;
    @Mock private br.com.saudeConecta.application.consulta.ConfiguracoesConsultaService configuracoesConsultaService;

    @InjectMocks
    private ConsultaEstatisticaService consultaEstatisticaService;

    private static final Long ORG_ID = 1L;
    private static final Long USUARIO_ID = 10L;

    private Organizacao organizacao;
    private Profissional profissional;
    private Paciente paciente;
    private Consulta consultaAgendada;

    @BeforeEach
    void setUp() {
        organizacao = new Organizacao();
        organizacao.setId(ORG_ID);

        profissional = new Profissional();
        profissional.setId(5L);
        profissional.setTempoConsultaMinutos(30);

        paciente = new Paciente();
        paciente.setPaciCodigo(20L);

        consultaAgendada = Consulta.builder()
                .id(100L)
                .organizacao(organizacao)
                .profissional(profissional)
                .paciente(paciente)
                .dataHora(LocalDateTime.now().plusDays(1))
                .duracaoMinutos(30)
                .status(StatusConsulta.AGENDADA)
                .build();
    }

    // ========== getEstatisticasDashboardAdminOrg ==========

    @Nested
    @DisplayName("getEstatisticasDashboardAdminOrg")
    class DashboardAdminOrg {

        @Test
        @DisplayName("Deve retornar zeros quando não há consultas")
        void deveRetornarZerosQuandoSemConsultas() {
            when(consultaRepository.findEstatisticasDashboardByOrganizacao(
                    eq(ORG_ID), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardAdminOrg(ORG_ID);

            assertThat(resultado.getConsultasHoje()).isZero();
            assertThat(resultado.getConsultasSemana()).isZero();
        }

        @Test
        @DisplayName("Deve agregar corretamente consultas AGENDADA de hoje")
        void deveAgregarConsultasAgendadasHoje() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{ StatusConsulta.AGENDADA, "HOJE", 4L });
            when(consultaRepository.findEstatisticasDashboardByOrganizacao(
                    eq(ORG_ID), any(), any(), any(), any())).thenReturn(rows);

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardAdminOrg(ORG_ID);

            assertThat(resultado.getConsultasHoje()).isEqualTo(4L);
            assertThat(resultado.getConsultasAguardando()).isEqualTo(4L);
            assertThat(resultado.getConsultasSemana()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Deve agregar corretamente consultas REALIZADA de hoje")
        void deveAgregarConsultasRealizadasHoje() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{ StatusConsulta.REALIZADA, "HOJE", 2L });
            when(consultaRepository.findEstatisticasDashboardByOrganizacao(
                    eq(ORG_ID), any(), any(), any(), any())).thenReturn(rows);

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardAdminOrg(ORG_ID);

            assertThat(resultado.getConsultasAtendidas()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Deve agregar corretamente consultas CANCELADA da semana")
        void deveAgregarCancelamentosDaSemana() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{ StatusConsulta.CANCELADA, "SEMANA", 3L });
            when(consultaRepository.findEstatisticasDashboardByOrganizacao(
                    eq(ORG_ID), any(), any(), any(), any())).thenReturn(rows);

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardAdminOrg(ORG_ID);

            assertThat(resultado.getCanceladosSemana()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Deve agregar corretamente consultas CONFIRMADA da semana")
        void deveAgregarConfirmadosDaSemana() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{ StatusConsulta.CONFIRMADA, "SEMANA", 5L });
            when(consultaRepository.findEstatisticasDashboardByOrganizacao(
                    eq(ORG_ID), any(), any(), any(), any())).thenReturn(rows);

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardAdminOrg(ORG_ID);

            assertThat(resultado.getConfirmadosSemana()).isEqualTo(5L);
        }
    }

    // ========== getEstatisticasDashboardProfissional ==========

    @Nested
    @DisplayName("getEstatisticasDashboardProfissional")
    class DashboardProfissional {

        @Test
        @DisplayName("Deve retornar zeros quando não há consultas do profissional")
        void deveRetornarZerosQuandoSemConsultasDoProfissional() {
            when(consultaRepository.findEstatisticasDashboardByProfissional(
                    eq(USUARIO_ID), any(), any(), any(), any())).thenReturn(Collections.emptyList());

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardProfissional(USUARIO_ID);

            assertThat(resultado.getConsultasHoje()).isZero();
            assertThat(resultado.getConsultasSemana()).isZero();
        }

        @Test
        @DisplayName("Deve agregar CONFIRMADA de hoje para o profissional")
        void deveAgregarConfirmadaDeHojeParaProfissional() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{ StatusConsulta.CONFIRMADA, "HOJE", 3L });
            when(consultaRepository.findEstatisticasDashboardByProfissional(
                    eq(USUARIO_ID), any(), any(), any(), any())).thenReturn(rows);

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardProfissional(USUARIO_ID);

            assertThat(resultado.getConsultasAguardando()).isEqualTo(3L);
            assertThat(resultado.getConfirmadosSemana()).isEqualTo(3L);
        }
    }

    // ========== getEstatisticasDashboardSuperAdmin ==========

    @Nested
    @DisplayName("getEstatisticasDashboardSuperAdmin")
    class DashboardSuperAdmin {

        @Test
        @DisplayName("Deve retornar zeros quando não há consultas globais")
        void deveRetornarZerosGlobal() {
            when(consultaRepository.findEstatisticasDashboardGlobal(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardSuperAdmin();

            assertThat(resultado.getConsultasSemana()).isZero();
        }

        @Test
        @DisplayName("Deve agregar REALIZADA de hoje no dashboard global")
        void deveAgregarRealizadaDeHojeGlobal() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{ StatusConsulta.REALIZADA, "HOJE", 6L });
            when(consultaRepository.findEstatisticasDashboardGlobal(any(), any(), any(), any()))
                    .thenReturn(rows);

            EstatisticasDashboardAdminOrgResponse resultado =
                    consultaEstatisticaService.getEstatisticasDashboardSuperAdmin();

            assertThat(resultado.getConsultasAtendidas()).isEqualTo(6L);
            assertThat(resultado.getConsultasHoje()).isEqualTo(6L);
        }
    }

    // ========== buscarEstatisticasPorMedicoEIntervalo ==========

    @Nested
    @DisplayName("buscarEstatisticasPorMedicoEIntervalo")
    class BuscarEstatisticasPorMedicoEIntervalo {

        @Test
        @DisplayName("Deve retornar estatísticas de consultas por médico e intervalo")
        void deveRetornarEstatisticasPorMedicoEIntervalo() {
            LocalDate inicio = LocalDate.now().minusDays(30);
            LocalDate fim = LocalDate.now();
            List<Object[]> estatisticas = new ArrayList<>();
            estatisticas.add(new Object[]{ StatusConsulta.REALIZADA, 5L });
            estatisticas.add(new Object[]{ StatusConsulta.CANCELADA, 2L });
            when(consultaRepository.findEstatisticasPorMedicoEIntervalo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(estatisticas);

            List<Object[]> resultado = consultaEstatisticaService.buscarEstatisticasPorMedicoEIntervalo(ORG_ID, USUARIO_ID, inicio, fim);

            assertThat(resultado).hasSize(2);
            verify(consultaRepository).findEstatisticasPorMedicoEIntervalo(eq(ORG_ID), eq(USUARIO_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver estatísticas")
        void deveRetornarListaVaziaQuandoSemEstatisticas() {
            LocalDate inicio = LocalDate.now().minusDays(7);
            LocalDate fim = LocalDate.now();
            when(consultaRepository.findEstatisticasPorMedicoEIntervalo(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Object[]> resultado = consultaEstatisticaService.buscarEstatisticasPorMedicoEIntervalo(ORG_ID, USUARIO_ID, inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarDuracoesConsultas ==========

    @Nested
    @DisplayName("buscarDuracoesConsultas")
    class BuscarDuracoesConsultas {

        @Test
        @DisplayName("Deve retornar lista de durações em minutos")
        void deveRetornarDuracoes() {
            LocalDate inicio = LocalDate.of(2024, 6, 1);
            LocalDate fim = LocalDate.of(2024, 6, 30);

            when(consultaRepository.findDuracoesConsultasPorMedicoEIntervalo(
                    eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(30, 45, 60));

            List<Integer> resultado = consultaEstatisticaService.buscarDuracoesConsultas(ORG_ID, 5L, inicio, fim);

            assertThat(resultado).containsExactly(30, 45, 60);
            verify(consultaRepository).findDuracoesConsultasPorMedicoEIntervalo(
                    eq(ORG_ID), eq(5L), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há consultas no período")
        void deveRetornarVazioQuandoSemConsultas() {
            LocalDate inicio = LocalDate.of(2024, 6, 1);
            LocalDate fim = LocalDate.of(2024, 6, 30);

            when(consultaRepository.findDuracoesConsultasPorMedicoEIntervalo(
                    eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Integer> resultado = consultaEstatisticaService.buscarDuracoesConsultas(ORG_ID, 5L, inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }
}
