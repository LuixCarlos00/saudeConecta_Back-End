package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.ConsultaHistorico;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
 import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.AtualizarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.EstatisticasDashboardAdminOrgResponse;
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
@DisplayName("ConsultaService - Testes unitários")
class ConsultaServiceTest {

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

    @InjectMocks
    private ConsultaService consultaService;

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

    // ========== buscarTodas ==========

    @Nested
    @DisplayName("buscarTodas")
    class BuscarTodas {

        @Test
        @DisplayName("Deve buscar consultas paginadas da organização atual")
        void deveBuscarConsultasPaginadasDaOrganizacao() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacao_Id(eq(ORG_ID), any())).thenReturn(mock(org.springframework.data.domain.Page.class));

            consultaService.buscarTodas(mock(org.springframework.data.domain.Pageable.class));

            verify(consultaRepository).findByOrganizacao_Id(eq(ORG_ID), any());
        }
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

            Optional<Consulta> resultado = consultaService.buscarPorId(100L);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Deve retornar Optional vazio quando ID não existir")
        void deveRetornarVazioQuandoIdNaoExistir() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            Optional<Consulta> resultado = consultaService.buscarPorId(999L);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== cadastrarConsultaByOrg ==========

    @Nested
    @DisplayName("cadastrarConsultaByOrg")
    class CadastrarConsulta {

        private AgendarConsultaRequest requestValido;

        @BeforeEach
        void setUp() {
            requestValido = new AgendarConsultaRequest(
                    5L, 20L, null,
                    LocalDateTime.now().plusDays(1),
                    45, "Observação teste", null, BigDecimal.TEN
            );
        }

        @Test
        @DisplayName("Deve cadastrar consulta com sucesso quando dados são válidos")
        void deveCadastrarConsultaComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                    eq(5L), eq(ORG_ID), any(), eq(StatusConsulta.AGENDADA))).thenReturn(false);
            when(consultaRepository.save(any(Consulta.class))).thenReturn(consultaAgendada);
            when(historicoRepository.save(any(ConsultaHistorico.class))).thenReturn(null);

            Consulta resultado = consultaService.cadastrarConsultaByOrg(requestValido);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(100L);
            verify(consultaRepository).save(any(Consulta.class));
        }

        @Test
        @DisplayName("Deve usar duração do profissional quando duracaoMinutos for nulo")
        void deveUsarDuracaoDoProfissionalQuandoNula() {
            AgendarConsultaRequest requestSemDuracao = new AgendarConsultaRequest(
                    5L, 20L, null,
                    LocalDateTime.now().plusDays(1),
                    null, null, null, null
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                    any(), any(), any(), any())).thenReturn(false);
            when(consultaRepository.save(any(Consulta.class))).thenAnswer(inv -> {
                Consulta c = inv.getArgument(0);
                assertThat(c.getDuracaoMinutos()).isEqualTo(30);
                return consultaAgendada;
            });
            when(historicoRepository.save(any())).thenReturn(null);

            consultaService.cadastrarConsultaByOrg(requestSemDuracao);
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não for encontrada")
        void deveLancarExcecaoQuandoOrganizacaoNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Organização não encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não for encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional não encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando paciente não for encontrado")
        void deveLancarExcecaoQuandoPacienteNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paciente não encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando já existe consulta agendada no mesmo horário")
        void deveLancarExcecaoQuandoHorarioOcupado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                    eq(5L), eq(ORG_ID), any(), eq(StatusConsulta.AGENDADA))).thenReturn(true);

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Já existe consulta agendada para este horário");
        }

        @Test
        @DisplayName("Deve associar especialidade quando especialidadeId for fornecido")
        void deveAssociarEspecialidadeQuandoFornecida() {
            AgendarConsultaRequest requestComEspecialidade = new AgendarConsultaRequest(
                    5L, 20L, 3L,
                    LocalDateTime.now().plusDays(1),
                    30, null, null, null
            );

            Especialidade especialidade = new Especialidade();
            especialidade.setId(3L);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                    any(), any(), any(), any())).thenReturn(false);
            when(especialidadeRepository.findById(3L)).thenReturn(Optional.of(especialidade));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(historicoRepository.save(any())).thenReturn(null);

            consultaService.cadastrarConsultaByOrg(requestComEspecialidade);

            verify(especialidadeRepository).findById(3L);
        }
    }

    // ========== concluirConsultabyOrg ==========

    @Nested
    @DisplayName("concluirConsultabyOrg")
    class ConcluirConsulta {

        @Test
        @DisplayName("Deve concluir consulta AGENDADA com sucesso")
        void deveConcluirConsultaAgendadaComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(consultaRepository.save(any(Consulta.class))).thenReturn(consultaAgendada);
            when(consultaRepository.findByIdAndOrganizacao_IdWithRelations(any(), any()))
                    .thenReturn(Optional.of(consultaAgendada));
            when(historicoRepository.save(any())).thenReturn(null);

            Consulta resultado = consultaService.concluirConsultabyOrg(100L);

            assertThat(resultado).isNotNull();
            assertThat(consultaAgendada.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
            verify(consultaRepository).save(consultaAgendada);
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for AGENDADA")
        void deveLancarExcecaoQuandoConsultaNaoAgendada() {
            consultaAgendada.setStatus(StatusConsulta.REALIZADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.concluirConsultabyOrg(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Apenas consultas AGENDADAS podem ser concluídas");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.concluirConsultabyOrg(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta não encontrada");
        }
    }

    // ========== atualizarStatus ==========

    @Nested
    @DisplayName("atualizarStatus - Transições permitidas e proibidas")
    class AtualizarStatus {

        @Test
        @DisplayName("AGENDADA → CONFIRMADA deve ser permitido")
        void devePermitirTransicaoAgendadaParaConfirmada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(consultaRepository.findByIdAndOrganizacao_IdWithRelations(any(), any()))
                    .thenReturn(Optional.of(consultaAgendada));
            when(historicoRepository.save(any())).thenReturn(null);

            Consulta resultado = consultaService.atualizarStatus(100L, StatusConsulta.CONFIRMADA, null);
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("AGENDADA → CANCELADA deve exigir motivo")
        void deveLancarExcecaoAoCancelarSemMotivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarStatus(100L, StatusConsulta.CANCELADA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Motivo é obrigatório");
        }

        @Test
        @DisplayName("AGENDADA → CANCELADA com motivo deve ser permitido")
        void devePermitirCancelamentoComMotivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(consultaRepository.findByIdAndOrganizacao_IdWithRelations(any(), any()))
                    .thenReturn(Optional.of(consultaAgendada));
            when(historicoRepository.save(any())).thenReturn(null);

            consultaService.atualizarStatus(100L, StatusConsulta.CANCELADA, "Paciente cancelou");
            assertThat(consultaAgendada.getMotivoCancelamento()).isEqualTo("Paciente cancelou");
        }

        @Test
        @DisplayName("AGENDADA → REALIZADA deve ser bloqueado")
        void deveBloquerarTransicaoAgendadaParaRealizada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarStatus(100L, StatusConsulta.REALIZADA, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AGENDADA só pode ir para CONFIRMADA ou CANCELADA");
        }

        @Test
        @DisplayName("CONFIRMADA → AGENDADA deve ser permitido")
        void devePermitirTransicaoConfirmadaParaAgendada() {
            consultaAgendada.setStatus(StatusConsulta.CONFIRMADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(consultaRepository.findByIdAndOrganizacao_IdWithRelations(any(), any()))
                    .thenReturn(Optional.of(consultaAgendada));
            when(historicoRepository.save(any())).thenReturn(null);

            Consulta resultado = consultaService.atualizarStatus(100L, StatusConsulta.AGENDADA, null);
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("CONFIRMADA → PAGO deve ser bloqueado")
        void deveBloquerarTransicaoConfirmadaParaPago() {
            consultaAgendada.setStatus(StatusConsulta.CONFIRMADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarStatus(100L, StatusConsulta.PAGO, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONFIRMADA só pode voltar para AGENDADA ou ir para CANCELADA");
        }

        @Test
        @DisplayName("REALIZADA → PAGO deve ser permitido")
        void devePermitirTransicaoRealizadaParaPago() {
            consultaAgendada.setStatus(StatusConsulta.REALIZADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(consultaRepository.findByIdAndOrganizacao_IdWithRelations(any(), any()))
                    .thenReturn(Optional.of(consultaAgendada));
            when(historicoRepository.save(any())).thenReturn(null);

            Consulta resultado = consultaService.atualizarStatus(100L, StatusConsulta.PAGO, null);
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("REALIZADA → AGENDADA deve ser bloqueado")
        void deveBloquerarTransicaoRealizadaParaAgendada() {
            consultaAgendada.setStatus(StatusConsulta.REALIZADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REALIZADA só pode ir para PAGO");
        }

        @Test
        @DisplayName("PAGO não pode ter status alterado")
        void deveBloquerarAlteracaoDeStatusPago() {
            consultaAgendada.setStatus(StatusConsulta.PAGO);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não podem ter o status alterado");
        }

        @Test
        @DisplayName("CANCELADA não pode ter status alterado")
        void deveBloquerarAlteracaoDeStatusCancelada() {
            consultaAgendada.setStatus(StatusConsulta.CANCELADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não podem ter o status alterado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.atualizarStatus(999L, StatusConsulta.CONFIRMADA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta não encontrada com ID: 999");
        }
    }

    // ========== contarAgendadasHoje ==========

    @Nested
    @DisplayName("contarAgendadasHoje")
    class ContarAgendadasHoje {

        @Test
        @DisplayName("Deve retornar contagem de consultas agendadas hoje")
        void deveRetornarContagemAgendadasHoje() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.countAgendadasHoje(ORG_ID)).thenReturn(5L);

            Long resultado = consultaService.contarAgendadasHoje();

            assertThat(resultado).isEqualTo(5L);
        }
    }

    // ========== estatísticas por usuário/org ==========

    @Nested
    @DisplayName("Estatísticas por profissional e organização")
    class EstatisticasPorProfissional {

        @Test
        @DisplayName("Deve contar consultas de hoje por usuário e organização")
        void deveContarConsultasHojePorUsuarioEOrg() {
            when(consultaRepository.countConsultasHojePorUsuarioEOrg(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(3L);

            Long resultado = consultaService.contarConsultasHojePorUsuarioEOrg(ORG_ID, USUARIO_ID);

            assertThat(resultado).isEqualTo(3L);
        }

        @Test
        @DisplayName("Deve contar consultas realizadas hoje por usuário e organização")
        void deveContarConsultasRealizadasHojePorUsuarioEOrg() {
            when(consultaRepository.countConsultasRealizadasHojePorUsuarioEOrg(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(2L);

            Long resultado = consultaService.contarConsultasRealizadasHojePorUsuarioEOrg(ORG_ID, USUARIO_ID);

            assertThat(resultado).isEqualTo(2L);
        }

        @Test
        @DisplayName("Deve contar consultas agendadas hoje por usuário e organização")
        void deveContarConsultasAgendadasHojePorUsuarioEOrg() {
            when(consultaRepository.countConsultasAgendadasHojePorUsuarioEOrg(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(1L);

            Long resultado = consultaService.contarConsultasAgendadasHojePorUsuarioEOrg(ORG_ID, USUARIO_ID);

            assertThat(resultado).isEqualTo(1L);
        }
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
                    consultaService.getEstatisticasDashboardAdminOrg(ORG_ID);

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
                    consultaService.getEstatisticasDashboardAdminOrg(ORG_ID);

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
                    consultaService.getEstatisticasDashboardAdminOrg(ORG_ID);

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
                    consultaService.getEstatisticasDashboardAdminOrg(ORG_ID);

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
                    consultaService.getEstatisticasDashboardAdminOrg(ORG_ID);

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
                    consultaService.getEstatisticasDashboardProfissional(USUARIO_ID);

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
                    consultaService.getEstatisticasDashboardProfissional(USUARIO_ID);

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
                    consultaService.getEstatisticasDashboardSuperAdmin();

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
                    consultaService.getEstatisticasDashboardSuperAdmin();

            assertThat(resultado.getConsultasAtendidas()).isEqualTo(6L);
            assertThat(resultado.getConsultasHoje()).isEqualTo(6L);
        }
    }

    // ========== verificarDisponibilidade ==========

    @Nested
    @DisplayName("verificarDisponibilidade")
    class VerificarDisponibilidade {

        @Test
        @DisplayName("Deve retornar true quando horário está ocupado")
        void deveRetornarTrueQuandoHorarioOcupado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                    eq(5L), eq(ORG_ID), any(), eq(StatusConsulta.AGENDADA))).thenReturn(true);

            boolean resultado = consultaService.verificarDisponibilidade("2025-12-01", "10:00", 5L);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false quando horário está disponível")
        void deveRetornarFalseQuandoHorarioDisponivel() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                    eq(5L), eq(ORG_ID), any(), eq(StatusConsulta.AGENDADA))).thenReturn(false);

            boolean resultado = consultaService.verificarDisponibilidade("2025-12-01", "10:00", 5L);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando data/hora for inválida")
        void deveRetornarFalseQuandoDataHoraInvalida() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);

            boolean resultado = consultaService.verificarDisponibilidade("data-invalida", "hora-invalida", 5L);

            assertThat(resultado).isFalse();
        }
    }

    // ========== deletarConsulta ==========

    @Nested
    @DisplayName("deletarConsulta")
    class DeletarConsulta {

        @Test
        @DisplayName("Deve deletar consulta sem prontuário com sucesso")
        void deveDeletarConsultaSemProntuarioComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(prontuarioDentistaRepository.findByConsultaId(100L))
                    .thenReturn(Collections.emptyList());
            when(prontuarioRepository.findByConsulta_Id(100L)).thenReturn(null);

            consultaService.deletarConsulta(100L);

            verify(termoAutorizacaoRepository).deleteByConsultaId(100L);
            verify(planejamentoTerapeuticoRepository).deleteByConsultaId(100L);
            verify(historicoRepository).deleteByConsultaId(100L);
            verify(consultaRepository).delete(consultaAgendada);
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.deletarConsulta(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta não encontrada");
        }

        @Test
        @DisplayName("Deve bloquear exclusão quando houver prontuário odontológico")
        void deveBloquerarExclusaoComProntuarioDentista() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(prontuarioDentistaRepository.findByConsultaId(100L))
                    .thenReturn(List.of(mock(br.com.saudeConecta.domain.prontuario.ProntuarioDentista.class)));

            assertThatThrownBy(() -> consultaService.deletarConsulta(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prontuário odontológico");
        }

        @Test
        @DisplayName("Deve bloquear exclusão quando houver prontuário médico")
        void deveBloquerarExclusaoComProntuarioMedico() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(prontuarioDentistaRepository.findByConsultaId(100L))
                    .thenReturn(Collections.emptyList());
            when(prontuarioRepository.findByConsulta_Id(100L))
                    .thenReturn(mock(br.com.saudeConecta.domain.prontuario.Prontuario.class));

            assertThatThrownBy(() -> consultaService.deletarConsulta(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prontuário médico");
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

            List<Consulta> resultado = consultaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "diario");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar consultas com período semanal")
        void deveBuscarConsultasPeriodoSemanal() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "semanal");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar consultas com período mensal")
        void deveBuscarConsultasPeriodoMensal() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "mensal");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar consultas com período anual")
        void deveBuscarConsultasPeriodoAnual() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "anual");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve usar periodo diario como default para tipo inválido")
        void deveUsarDiarioComoDefaultParaTipoInvalido() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasPorMedicoEPeriodo(USUARIO_ID, "invalido");

            assertThat(resultado).isEmpty();
            verify(consultaRepository).findConsultasPorMedicoEPeriodo(eq(ORG_ID), eq(USUARIO_ID), any(), any());
        }
    }

    // ========== estatísticas simples ==========

    @Nested
    @DisplayName("Estatísticas simples (por organização)")
    class EstatisticasSimples {

        @Test
        @DisplayName("Deve retornar consultas de hoje por organizacao")
        void deveRetornarConsultasHojeByOrg() {
            when(consultaRepository.countByOrganizacao_IdAndDataHoraBetween(eq(ORG_ID), any(), any()))
                    .thenReturn(7L);

            Long resultado = consultaService.getEstatisticaConsultasHojeByAdmiOrg(ORG_ID);

            assertThat(resultado).isEqualTo(7L);
        }

        @Test
        @DisplayName("Deve retornar consultas atendidas hoje por organização")
        void deveRetornarConsultasAtendidasHojeByOrg() {
            when(consultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween(
                    eq(ORG_ID), eq(StatusConsulta.REALIZADA), any(), any())).thenReturn(3L);

            Long resultado = consultaService.getEstatisticaConsultasAtendidasByAdmiOrg(ORG_ID);

            assertThat(resultado).isEqualTo(3L);
        }

        @Test
        @DisplayName("Deve retornar consultas agendadas hoje por organização")
        void deveRetornarConsultasAgendadasHojeByOrg() {
            when(consultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween(
                    eq(ORG_ID), eq(StatusConsulta.AGENDADA), any(), any())).thenReturn(4L);

            Long resultado = consultaService.getEstatisticasConsultaAgendadasHojeByOrd(ORG_ID);

            assertThat(resultado).isEqualTo(4L);
        }

        @Test
        @DisplayName("Deve retornar contagem total de consultas hoje (global)")
        void deveRetornarTotalConsultasHojeGlobal() {
            when(consultaRepository.countByDataHoraBetween(any(), any())).thenReturn(10L);

            Long resultado = consultaService.contarTodasConsultasHoje();

            assertThat(resultado).isEqualTo(10L);
        }

        @Test
        @DisplayName("Deve retornar contagem de consultas realizadas hoje (global)")
        void deveRetornarConsultasRealizadasHojeGlobal() {
            when(consultaRepository.countByStatusAndDataHoraBetween(
                    eq(StatusConsulta.REALIZADA), any(), any())).thenReturn(2L);

            Long resultado = consultaService.contarTodasConsultasRealizadasHoje();

            assertThat(resultado).isEqualTo(2L);
        }

        @Test
        @DisplayName("Deve retornar contagem de consultas agendadas hoje (global)")
        void deveRetornarConsultasAgendadasHojeGlobal() {
            when(consultaRepository.countByStatusAndDataHoraBetween(
                    eq(StatusConsulta.AGENDADA), any(), any())).thenReturn(8L);

            Long resultado = consultaService.contarTodasConsultasAgendadasHoje();

            assertThat(resultado).isEqualTo(8L);
        }

        @Test
        @DisplayName("Deve contar consultas da semana por organização e profissional")
        void deveContarConsultasSemana() {
            when(consultaRepository.countConsultasPorMedicoEIntervalo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(6L);

            Long resultado = consultaService.contarConsultasSemana(ORG_ID, 5L);

            assertThat(resultado).isEqualTo(6L);
        }

        @Test
        @DisplayName("Deve contar consultas da semana globalmente")
        void deveContarConsultasSemanaGlobal() {
            when(consultaRepository.countConsultasSemanaGlobal(any(), any())).thenReturn(15L);

            Long resultado = consultaService.contarConsultasSemanaGlobal();

            assertThat(resultado).isEqualTo(15L);
        }
    }

    // ========== atualizarConsultaByOrg ==========

    @Nested
    @DisplayName("atualizarConsultaByOrg")
    class AtualizarConsultaByOrg {

        private AtualizarConsultaRequest requestAtualizar;

        @BeforeEach
        void setUp() {
            requestAtualizar = new AtualizarConsultaRequest(
                    5L, 20L, null,
                    LocalDateTime.now().plusDays(2),
                    45, "Obs atualizada", null,
                    BigDecimal.valueOf(200), null
            );
        }

        @Test
        @DisplayName("Deve atualizar consulta com sucesso")
        void deveAtualizarConsultaComSucesso() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.findByProfissional_IdAndOrganizacao_IdAndDataHora(eq(5L), eq(ORG_ID), any()))
                    .thenReturn(Collections.emptyList());
            when(consultaRepository.save(any(Consulta.class))).thenReturn(consultaAgendada);
            when(historicoRepository.save(any())).thenReturn(null);

            Consulta resultado = consultaService.atualizarConsultaByOrg(100L, requestAtualizar);

            assertThat(resultado).isNotNull();
            verify(consultaRepository).save(consultaAgendada);
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for encontrada")
        void deveLancarExcecaoConsultaNaoEncontrada() {
            when(consultaRepository.findByIdAndOrganizacao_Id(anyLong(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(999L, requestAtualizar))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta não encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não for encontrado")
        void deveLancarExcecaoProfissionalNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional não encontrado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando horário estiver ocupado por outra consulta")
        void deveLancarExcecaoHorarioOcupadoPorOutraConsulta() {
            Consulta outraConsulta = new Consulta();
            outraConsulta.setId(999L);
            outraConsulta.setStatus(StatusConsulta.AGENDADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.findByProfissional_IdAndOrganizacao_IdAndDataHora(eq(5L), eq(ORG_ID), any()))
                    .thenReturn(List.of(outraConsulta));

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("consulta agendada");
        }

        @Test
        @DisplayName("Deve atualizar status quando request.status for informado")
        void deveAtualizarStatusQuandoInformado() {
            AtualizarConsultaRequest requestComStatus = new AtualizarConsultaRequest(
                    5L, 20L, null,
                    LocalDateTime.now().plusDays(2),
                    null, null, null, null, "CONFIRMADA"
            );

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(consultaRepository.findByProfissional_IdAndOrganizacao_IdAndDataHora(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(historicoRepository.save(any())).thenReturn(null);

            consultaService.atualizarConsultaByOrg(100L, requestComStatus);

            assertThat(consultaAgendada.getStatus()).isEqualTo(StatusConsulta.CONFIRMADA);
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

            List<Consulta> resultado = consultaService.buscarConsultasPorOrganizacaoEIntervalo(ORG_ID, inicio, fim);

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

            List<Consulta> resultado = consultaService.buscarConsultasPorOrganizacaoEIntervalo(ORG_ID, inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== getEstatisticasSemanaPorOrganizacao ==========

    @Nested
    @DisplayName("getEstatisticasSemanaPorOrganizacao")
    class GetEstatisticasSemanaPorOrganizacao {

        @Test
        @DisplayName("Deve retornar contagem de consultas da semana por organização")
        void deveRetornarContagemSemana() {
            when(consultaRepository.countConsultasSemanaByOrganizacao(eq(ORG_ID), any(), any()))
                    .thenReturn(12L);

            Long resultado = consultaService.getEstatisticasSemanaPorOrganizacao(ORG_ID);

            assertThat(resultado).isEqualTo(12L);
            verify(consultaRepository).countConsultasSemanaByOrganizacao(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar zero quando não houver consultas na semana")
        void deveRetornarZeroQuandoSemConsultas() {
            when(consultaRepository.countConsultasSemanaByOrganizacao(eq(ORG_ID), any(), any()))
                    .thenReturn(0L);

            Long resultado = consultaService.getEstatisticasSemanaPorOrganizacao(ORG_ID);

            assertThat(resultado).isZero();
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

            List<Object[]> resultado = consultaService.buscarEstatisticasPorMedicoEIntervalo(ORG_ID, USUARIO_ID, inicio, fim);

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

            List<Object[]> resultado = consultaService.buscarEstatisticasPorMedicoEIntervalo(ORG_ID, USUARIO_ID, inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== pesquisarClinicasEmIntervaloDeDatas ==========

    @Nested
    @DisplayName("pesquisarClinicasEmIntervaloDeDatas")
    class PesquisarClinicasEmIntervaloDeDatas {

        private final LocalDate INICIO = LocalDate.now().minusDays(7);
        private final LocalDate FIM = LocalDate.now();

        @Test
        @DisplayName("Deve buscar todas as consultas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
                    eq(ORG_ID), eq(5L), isNull(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarClinicasEmIntervaloDeDatas(5L, INICIO, FIM, "ALL");

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

            List<Consulta> resultado = consultaService.pesquisarClinicasEmIntervaloDeDatas(5L, INICIO, FIM, "AGENDADA");

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

            List<Consulta> resultado = consultaService.pesquisarClinicasEmIntervaloDeDatas(5L, INICIO, FIM, null);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve lancar erro quando status for invalido")
        void deveLancarErroQuandoStatusInvalido() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);

            assertThatThrownBy(() ->
                    consultaService.pesquisarClinicasEmIntervaloDeDatas(5L, INICIO, FIM, "INEXISTENTE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Status de consulta invalido");
        }
    }

    // ========== pesquisarMedicoEspecialidadeEmIntervaloDeDatas ==========

    @Nested
    @DisplayName("pesquisarMedicoEspecialidadeEmIntervaloDeDatas")
    class PesquisarMedicoEspecialidadeEmIntervaloDeDatas {

        private final LocalDate INICIO = LocalDate.now().minusDays(7);
        private final LocalDate FIM = LocalDate.now();

        @Test
        @DisplayName("Deve buscar todas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq(5L), eq("Cardiologia"), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarMedicoEspecialidadeEmIntervaloDeDatas(
                    5L, "Cardiologia", INICIO, FIM, "ALL");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar por especialidade e status")
        void deveFiltrarPorEspecialidadeEStatus() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq(5L), eq("Cardiologia"), eq(StatusConsulta.CONFIRMADA), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarMedicoEspecialidadeEmIntervaloDeDatas(
                    5L, "Cardiologia", INICIO, FIM, "CONFIRMADA");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar sem filtro de status quando status for nulo")
        void deveBuscarSemFiltroQuandoStatusNulo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq(5L), eq("Cardiologia"), isNull(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.pesquisarMedicoEspecialidadeEmIntervaloDeDatas(
                    5L, "Cardiologia", INICIO, FIM, null);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== pesquisarEspecialidadeEmIntervaloDeDatas ==========

    @Nested
    @DisplayName("pesquisarEspecialidadeEmIntervaloDeDatas")
    class PesquisarEspecialidadeEmIntervaloDeDatas {

        private final LocalDate INICIO = LocalDate.now().minusDays(7);
        private final LocalDate FIM = LocalDate.now();

        @Test
        @DisplayName("Deve buscar todas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq("Neurologia"), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarEspecialidadeEmIntervaloDeDatas(
                    "Neurologia", INICIO, FIM, "ALL");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar por especialidade e status")
        void deveFiltrarPorEspecialidadeEStatus() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq("Neurologia"), eq(StatusConsulta.AGENDADA), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarEspecialidadeEmIntervaloDeDatas(
                    "Neurologia", INICIO, FIM, "AGENDADA");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar sem filtro quando status for nulo")
        void deveBuscarSemFiltroQuandoStatusNulo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
                    eq(ORG_ID), eq("Neurologia"), isNull(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.pesquisarEspecialidadeEmIntervaloDeDatas(
                    "Neurologia", INICIO, FIM, null);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== pesquisarPorProfissionalEEspecialidade ==========

    @Nested
    @DisplayName("pesquisarPorProfissionalEEspecialidade")
    class PesquisarPorProfissionalEEspecialidade {

        @Test
        @DisplayName("Deve buscar todas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingWithRelations(
                    eq(ORG_ID), eq(5L), eq("Pediatria")))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarPorProfissionalEEspecialidade(5L, "Pediatria", "ALL");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar por status quando informado")
        void deveFiltrarPorStatus() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
                    eq(ORG_ID), eq(5L), eq("Pediatria"), eq(StatusConsulta.REALIZADA)))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarPorProfissionalEEspecialidade(5L, "Pediatria", "REALIZADA");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar sem filtro quando status for nulo")
        void deveBuscarSemFiltroQuandoStatusNulo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
                    eq(ORG_ID), eq(5L), eq("Pediatria"), isNull()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.pesquisarPorProfissionalEEspecialidade(5L, "Pediatria", null);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== pesquisarPorEspecialidade ==========

    @Nested
    @DisplayName("pesquisarPorEspecialidade")
    class PesquisarPorEspecialidade {

        @Test
        @DisplayName("Deve buscar todas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingWithRelations(
                    eq(ORG_ID), eq("Ortopedia")))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarPorEspecialidade("Ortopedia", "ALL");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar por status quando informado")
        void deveFiltrarPorStatus() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
                    eq(ORG_ID), eq("Ortopedia"), eq(StatusConsulta.AGENDADA)))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarPorEspecialidade("Ortopedia", "AGENDADA");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar sem filtro quando status for nulo")
        void deveBuscarSemFiltroQuandoStatusNulo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
                    eq(ORG_ID), eq("Ortopedia"), isNull()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.pesquisarPorEspecialidade("Ortopedia", null);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== pesquisarPorEspecialidadeEOrganizacao ==========

    @Nested
    @DisplayName("pesquisarPorEspecialidadeEOrganizacao")
    class PesquisarPorEspecialidadeEOrganizacao {

        @Test
        @DisplayName("Deve buscar todas quando status for ALL")
        void deveBuscarTodasQuandoStatusAll() {
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingWithRelations(
                    eq(ORG_ID), eq("Dermatologia")))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarPorEspecialidadeEOrganizacao(ORG_ID, "Dermatologia", "ALL");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve filtrar por status quando informado")
        void deveFiltrarPorStatus() {
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
                    eq(ORG_ID), eq("Dermatologia"), eq(StatusConsulta.CONFIRMADA)))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.pesquisarPorEspecialidadeEOrganizacao(ORG_ID, "Dermatologia", "CONFIRMADA");

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve buscar sem filtro quando status for nulo")
        void deveBuscarSemFiltroQuandoStatusNulo() {
            when(consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
                    eq(ORG_ID), eq("Dermatologia"), isNull()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.pesquisarPorEspecialidadeEOrganizacao(ORG_ID, "Dermatologia", null);

            assertThat(resultado).isEmpty();
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
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarConsultasHoje(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas hoje")
        void deveRetornarListaVaziaQuandoSemConsultasHoje() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasHoje(null);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar consultas do dia informado quando data for passada")
        void deveRetornarConsultasDoDiaInformado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            LocalDate dataInformada = LocalDate.of(2024, 3, 10);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(
                    eq(ORG_ID), eq(dataInformada.atStartOfDay()), eq(dataInformada.atTime(23, 59, 59))))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarConsultasHoje(dataInformada);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(
                    eq(ORG_ID), eq(dataInformada.atStartOfDay()), eq(dataInformada.atTime(23, 59, 59)));
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
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarConsultasDaSemanaAtual(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas na semana")
        void deveRetornarListaVaziaQuandoSemConsultasNaSemana() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasDaSemanaAtual(null);

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
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarConsultasDoMesAtual(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no mês")
        void deveRetornarListaVaziaQuandoSemConsultasNoMes() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasDoMesAtual(null);

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

            List<Consulta> resultado = consultaService.buscarConsultasDoAnoAtual();

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no ano")
        void deveRetornarListaVaziaQuandoSemConsultasNoAno() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarConsultasDoAnoAtual();

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorProfissional ==========

    @Nested
    @DisplayName("buscarPorProfissional")
    class BuscarPorProfissional {

        @Test
        @DisplayName("Deve retornar consultas do profissional na organização")
        void deveRetornarConsultasDoProfissional() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacao_IdAndProfissional_IdWithRelations(ORG_ID, 5L))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarPorProfissional(5L);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacao_IdAndProfissional_IdWithRelations(ORG_ID, 5L);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando profissional não tiver consultas")
        void deveRetornarListaVaziaQuandoProfissionalSemConsultas() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacao_IdAndProfissional_IdWithRelations(ORG_ID, 5L))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarPorProfissional(5L);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarAgendaDia ==========

    @Nested
    @DisplayName("buscarAgendaDia")
    class BuscarAgendaDia {

        @Test
        @DisplayName("Deve retornar agenda do profissional para a data informada")
        void deveRetornarAgendaDia() {
            LocalDate data = LocalDate.now();
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAgendaDia(ORG_ID, 5L, data))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarAgendaDia(5L, data);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findAgendaDia(ORG_ID, 5L, data);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver agenda no dia")
        void deveRetornarListaVaziaQuandoSemAgendaNoDia() {
            LocalDate data = LocalDate.now().plusDays(10);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAgendaDia(ORG_ID, 5L, data))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarAgendaDia(5L, data);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorPeriodo ==========

    @Nested
    @DisplayName("buscarPorPeriodo")
    class BuscarPorPeriodo {

        @Test
        @DisplayName("Deve retornar consultas no período informado")
        void deveRetornarConsultasNoPeriodo() {
            LocalDateTime inicio = LocalDateTime.now().minusDays(7);
            LocalDateTime fim = LocalDateTime.now();
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(ORG_ID, inicio, fim))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarPorPeriodo(inicio, fim);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(ORG_ID, inicio, fim);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no período")
        void deveRetornarListaVaziaQuandoSemConsultasNoPeriodo() {
            LocalDateTime inicio = LocalDateTime.now().minusDays(60);
            LocalDateTime fim = LocalDateTime.now().minusDays(30);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(ORG_ID, inicio, fim))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarPorPeriodo(inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarPorPaciente ==========

    @Nested
    @DisplayName("buscarPorPaciente")
    class BuscarPorPaciente {

        @Test
        @DisplayName("Deve retornar consultas do paciente na organização")
        void deveRetornarConsultasDoPaciente() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacao_IdAndPaciente_PaciCodigoWithRelations(ORG_ID, 20L))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarPorPaciente(20L);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacao_IdAndPaciente_PaciCodigoWithRelations(ORG_ID, 20L);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando paciente não tiver consultas")
        void deveRetornarListaVaziaQuandoPacienteSemConsultas() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacao_IdAndPaciente_PaciCodigoWithRelations(ORG_ID, 20L))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarPorPaciente(20L);

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

            List<Consulta> resultado = consultaService.buscarConsultasPorIntervalo(inicio, fim, "ALL");

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

            List<Consulta> resultado = consultaService.buscarConsultasPorIntervalo(inicio, fim, "AGENDADA");

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

            List<Consulta> resultado = consultaService.buscarConsultasPorIntervalo(inicio, fim, null);

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

            List<Consulta> resultado = consultaService.buscarComFiltrosDinamicos(
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

            List<Consulta> resultado = consultaService.buscarComFiltrosDinamicos(
                    null, null, null, null, null);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há resultados")
        void deveRetornarVazioQuandoSemResultados() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(Collections.emptyList());

            List<Consulta> resultado = consultaService.buscarComFiltrosDinamicos(
                    1L, null, null, null, null);

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

            List<Integer> resultado = consultaService.buscarDuracoesConsultas(ORG_ID, 5L, inicio, fim);

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

            List<Integer> resultado = consultaService.buscarDuracoesConsultas(ORG_ID, 5L, inicio, fim);

            assertThat(resultado).isEmpty();
        }
    }

    // ========== buscarHorariosOcupados ==========

    @Nested
    @DisplayName("buscarHorariosOcupados")
    class BuscarHorariosOcupados {

        @Test
        @DisplayName("Deve retornar horários ocupados para o profissional na data")
        void deveRetornarHorariosOcupados() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findHorariosOcupados(eq(ORG_ID), eq(5L), any()))
                    .thenReturn(List.of("08:00", "09:00", "14:00"));

            List<String> resultado = consultaService.buscarHorariosOcupados(5L, "2024-06-15");

            assertThat(resultado).containsExactly("08:00", "09:00", "14:00");
            verify(consultaRepository).findHorariosOcupados(eq(ORG_ID), eq(5L), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando profissional não tem consultas na data")
        void deveRetornarVazioQuandoSemHorarios() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findHorariosOcupados(eq(ORG_ID), eq(5L), any()))
                    .thenReturn(Collections.emptyList());

            List<String> resultado = consultaService.buscarHorariosOcupados(5L, "2024-06-15");

            assertThat(resultado).isEmpty();
        }
    }
}
