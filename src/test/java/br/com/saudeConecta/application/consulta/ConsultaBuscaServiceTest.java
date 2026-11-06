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
@DisplayName("ConsultaBuscaService - leituras de consultas da organização")
class ConsultaBuscaServiceTest {

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
    private ConsultaBuscaService consultaBuscaService;

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

    // ========== buscarPorId ==========

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar consulta quando ID existir na organização")
        void deveRetornarConsultaQuandoIdExistir() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            Optional<Consulta> resultado = consultaBuscaService.buscarPorId(100L);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Deve retornar Optional vazio quando ID não existir")
        void deveRetornarVazioQuandoIdNaoExistir() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            Optional<Consulta> resultado = consultaBuscaService.buscarPorId(999L);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("buscarPorIdComRelacoes usa a query com fetch e filtra pela organização do token")
        void comRelacoesUsaFetchDaOrganizacao() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_IdWithRelations(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThat(consultaBuscaService.buscarPorIdComRelacoes(100L)).contains(consultaAgendada);
            verify(consultaRepository, never()).findByIdAndOrganizacao_Id(anyLong(), any());
        }
    }

    // ========== buscarConsultasPorMedicoEPeriodo ==========

    @Nested
    @DisplayName("buscarConsultasPorMedicoEPeriodo")
    class BuscarConsultasPorMedicoEPeriodo {

        @Test
        @DisplayName("Deve buscar consultas com período diario")
        void deveBuscarConsultasPeriodoDiario() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "diario");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar consultas com período semanal")
        void deveBuscarConsultasPeriodoSemanal() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "semanal");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar consultas com período mensal")
        void deveBuscarConsultasPeriodoMensal() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "mensal");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar consultas com período anual")
        void deveBuscarConsultasPeriodoAnual() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "anual");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve usar periodo diario como default para tipo inválido")
        void deveUsarDiarioComoDefaultParaTipoInvalido() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "invalido");

            assertThat(resultado).isEmpty();
            verify(consultaRepository).findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any());
        }
    }

    // ========== buscarConsultasPorOrganizacaoEIntervalo ==========

    @Nested
    @DisplayName("buscarConsultasPorOrganizacaoEIntervalo")
    class BuscarConsultasPorOrganizacaoEIntervalo {

        @Test
        @DisplayName("Deve retornar lista de consultas no intervalo")
        void deveRetornarConsultasNoIntervalo() {
            LocalDate inicio = LocalDate.now().minusDays(7);
            LocalDate fim = LocalDate.now();
            when(consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(eq(ORG_ID), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorOrganizacaoEIntervalo(ORG_ID, inicio, fim);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndDataHoraBetweenWithRelations(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no intervalo")
        void deveRetornarListaVaziaQuandoSemConsultas() {
            LocalDate inicio = LocalDate.now().minusDays(30);
            LocalDate fim = LocalDate.now().minusDays(15);
            when(consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorOrganizacaoEIntervalo(ORG_ID, inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== pesquisarClinicasEmIntervaloDeDatas ==========

    @Nested
    @DisplayName("pesquisarClinicasEmIntervaloDeDatas")
    class PesquisarClinicasEmIntervaloDeDatas {

        private final LocalDate inicio = LocalDate.now().minusDays(7);
        private final LocalDate fim = LocalDate.now();

        @Test
        @DisplayName("Deve buscar todas as consultas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
                    eq(ORG_ID), eq(5L), isNull(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.pesquisarClinicasEmIntervaloDeDatas(5L, inicio, fim, "ALL");

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
                    eq(ORG_ID), eq(5L), isNull(), any(), any());
        }

        @Test
        @DisplayName("Deve filtrar por status específico")
        void deveFiltrarPorStatus() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
                    eq(ORG_ID), eq(5L), eq(StatusConsulta.AGENDADA), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.pesquisarClinicasEmIntervaloDeDatas(5L, inicio, fim, "AGENDADA");

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
                    eq(ORG_ID), eq(5L), eq(StatusConsulta.AGENDADA), any(), any());
        }

        @Test
        @DisplayName("Deve buscar sem filtro de status quando status for nulo")
        void deveBuscarSemFiltroQuandoStatusNulo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
                    eq(ORG_ID), eq(5L), isNull(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.pesquisarClinicasEmIntervaloDeDatas(5L, inicio, fim, null);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve lancar erro quando status for invalido")
        void deveLancarErroQuandoStatusInvalido() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);

            assertThatThrownBy(() ->
                    consultaBuscaService.pesquisarClinicasEmIntervaloDeDatas(5L, inicio, fim, "INEXISTENTE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Status de consulta invalido");
        }
    }

    // ========== buscarConsultasHoje ==========

    @Nested
    @DisplayName("buscarConsultasHoje")
    class BuscarConsultasHoje {

        @Test
        @DisplayName("Deve retornar consultas de hoje da organização")
        void deveRetornarConsultasHoje() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasHoje(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas hoje")
        void deveRetornarListaVaziaQuandoSemConsultasHoje() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasHoje(null);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar consultas do dia informado quando data for passada")
        void deveRetornarConsultasDoDiaInformado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            LocalDate dataInformada = LocalDate.of(2024, 3, 10);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(
                    eq(ORG_ID), any(), eq(dataInformada.atStartOfDay()), eq(dataInformada.atTime(23, 59, 59))))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasHoje(dataInformada);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(
                    eq(ORG_ID), any(), eq(dataInformada.atStartOfDay()), eq(dataInformada.atTime(23, 59, 59)));
        }
    }

    // ========== buscarConsultasDaSemanaAtual ==========

    @Nested
    @DisplayName("buscarConsultasDaSemanaAtual")
    class BuscarConsultasDaSemanaAtual {

        @Test
        @DisplayName("Deve retornar consultas da semana atual")
        void deveRetornarConsultasDaSemana() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasDaSemanaAtual(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas na semana")
        void deveRetornarListaVaziaQuandoSemConsultasNaSemana() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasDaSemanaAtual(null);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarConsultasDoMesAtual ==========

    @Nested
    @DisplayName("buscarConsultasDoMesAtual")
    class BuscarConsultasDoMesAtual {

        @Test
        @DisplayName("Deve retornar consultas do mês atual")
        void deveRetornarConsultasDoMes() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasDoMesAtual(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no mês")
        void deveRetornarListaVaziaQuandoSemConsultasNoMes() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasDoMesAtual(null);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarConsultasDoAnoAtual ==========

    @Nested
    @DisplayName("buscarConsultasDoAnoAtual")
    class BuscarConsultasDoAnoAtual {

        @Test
        @DisplayName("Deve retornar consultas do ano atual")
        void deveRetornarConsultasDoAno() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasDoAnoAtual();

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no ano")
        void deveRetornarListaVaziaQuandoSemConsultasNoAno() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarConsultasDoAnoAtual();

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarConsultasPorIntervalo ==========

    @Nested
    @DisplayName("buscarConsultasPorIntervalo")
    class BuscarConsultasPorIntervalo {

        @Test
        @DisplayName("Deve buscar todas as consultas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 1, 31);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), isNull(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorIntervalo(inicio, fim, "ALL");

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), isNull(), any(), any());
        }

        @Test
        @DisplayName("Deve filtrar por status específico quando informado")
        void deveFiltrarPorStatusEspecifico() {
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 1, 31);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq(StatusConsulta.AGENDADA), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorIntervalo(inicio, fim, "AGENDADA");

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq(StatusConsulta.AGENDADA), any(), any());
        }

        @Test
        @DisplayName("Deve buscar sem filtro de status quando status for nulo")
        void deveBuscarSemStatusQuandoNulo() {
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 1, 31);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), isNull(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarConsultasPorIntervalo(inicio, fim, null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), isNull(), any(), any());
        }
    }

    // ========== buscarComFiltrosDinamicos ==========

    @Nested
    @DisplayName("buscarComFiltrosDinamicos")
    class BuscarComFiltrosDinamicos {

        @Test
        @DisplayName("Deve buscar com todos os filtros preenchidos")
        void deveBuscarComTodosFiltros() {
            LocalDateTime inicio = LocalDateTime.of(2024, 6, 1, 0, 0);
            LocalDateTime fim = LocalDateTime.of(2024, 6, 30, 23, 59);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarComFiltrosDinamicos(
                    5L, "Cardiologia", inicio, fim, List.of(StatusConsulta.AGENDADA));

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class));
        }

        @Test
        @DisplayName("Deve buscar com filtros nulos (sem restrições)")
        void deveBuscarComFiltrosNulos() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaBuscaService.buscarComFiltrosDinamicos(
                    null, null, null, null, null);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há resultados")
        void deveRetornarVazioQuandoSemResultados() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaBuscaService.buscarComFiltrosDinamicos(
                    1L, null, null, null, null);

            assertThat(resultado).isEmpty();
        }
    }
}
