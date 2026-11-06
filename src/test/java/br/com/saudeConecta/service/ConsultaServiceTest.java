package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.pagamento.FormaPagamento;
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
    @Mock private br.com.saudeConecta.service.ConfiguracoesConsultaService configuracoesConsultaService;

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
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
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
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
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
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paciente não encontrado");
        }

        @Test
        @DisplayName("Item 4 - paciente de outra clínica não pode ser agendado (busca filtrada pela org do token)")
        void naoAgendaPacienteDeOutraClinica() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            // O paciente 20 existe, mas em outra clínica: a busca por (id, org do token) não o encontra
            lenient().when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paciente não encontrado");
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Forma de pagamento de outra clínica não pode ser usada ao agendar")
        void naoAgendaComFormaPagamentoDeOutraClinica() {
            prepararCadastro();
            AgendarConsultaRequest comFormaAlheia = new AgendarConsultaRequest(
                    5L, 20L, null, LocalDateTime.now().plusDays(1), 45, "Observação teste", 99L, BigDecimal.TEN);
            // A forma 99 existe, mas é de outra clínica: a busca por (id, org do token) não a encontra
            lenient().when(formaPagamentoRepository.findById(99L)).thenReturn(Optional.of(new FormaPagamento()));
            when(formaPagamentoRepository.findDisponivelParaOrganizacao(99L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(comFormaAlheia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Forma de pagamento nao encontrada");
            verify(consultaRepository, never()).save(any());
        }

        private void prepararCadastro() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
        }

        private Consulta existente(LocalDateTime inicio, Integer duracao, StatusConsulta status) {
            return Consulta.builder().id(200L).organizacao(organizacao).profissional(profissional)
                    .paciente(paciente).dataHora(inicio).duracaoMinutos(duracao).status(status).build();
        }

        private AgendarConsultaRequest requestEm(LocalDateTime inicio, Integer duracao) {
            return new AgendarConsultaRequest(5L, 20L, null, inicio, duracao, null, null, null);
        }

        @Test
        @DisplayName("Deve barrar consulta que sobrepõe parcialmente uma AGENDADA (09:00 60min x 09:30)")
        void deveBarrarSobreposicaoComAgendada() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove, 60, StatusConsulta.AGENDADA)));

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestEm(nove.plusMinutes(30), 30)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Já existe consulta agendada para este horário");
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve barrar sobreposição com consulta CONFIRMADA (fluxo acelerado)")
        void deveBarrarSobreposicaoComConfirmada() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove, 60, StatusConsulta.CONFIRMADA)));

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestEm(nove, 30)))
                    .isInstanceOf(IllegalStateException.class);
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve barrar novo intervalo que engloba consulta existente")
        void deveBarrarIntervaloQueEngloba() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove.plusMinutes(15), 15, StatusConsulta.AGENDADA)));

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestEm(nove, 60)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Deve usar 30 min quando a consulta existente não tem duração")
        void deveUsarFallbackDeDuracaoDaExistente() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove, null, StatusConsulta.AGENDADA)));

            assertThatThrownBy(() -> consultaService.cadastrarConsultaByOrg(requestEm(nove.plusMinutes(29), 30)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Deve permitir consulta encostada (09:00-10:00 e nova às 10:00)")
        void devePermitirConsultaEncostada() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove, 60, StatusConsulta.AGENDADA)));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaService.cadastrarConsultaByOrg(requestEm(nove.plusMinutes(60), 30));

            verify(consultaRepository).save(any(Consulta.class));
        }

        @Test
        @DisplayName("Deve permitir consulta que termina exatamente quando a existente começa")
        void devePermitirConsultaQueTerminaQuandoExistenteComeca() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove, 60, StatusConsulta.AGENDADA)));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaService.cadastrarConsultaByOrg(requestEm(nove.minusMinutes(30), 30));

            verify(consultaRepository).save(any(Consulta.class));
        }

        @Test
        @DisplayName("Deve travar o profissional (lock pessimista) antes de checar conflito e salvar")
        void deveTravarProfissionalAntesDeSalvar() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaService.cadastrarConsultaByOrg(requestEm(nove, 30));

            org.mockito.InOrder ordem = inOrder(profissionalRepository, consultaRepository);
            ordem.verify(profissionalRepository).buscarParaAgendamentoComLock(5L, ORG_ID);
            ordem.verify(consultaRepository).findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any());
            ordem.verify(consultaRepository).save(any(Consulta.class));
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
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
            when(especialidadeRepository.findById(3L)).thenReturn(Optional.of(especialidade));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);
            when(historicoRepository.save(any())).thenReturn(null);

            consultaService.cadastrarConsultaByOrg(requestComEspecialidade);

            verify(especialidadeRepository).findById(3L);
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

        private Consulta consultaAs(int hora, int minuto, int duracao, StatusConsulta status) {
            return Consulta.builder().id(200L).organizacao(organizacao).profissional(profissional).paciente(paciente)
                    .dataHora(LocalDateTime.of(2025, 12, 1, hora, minuto)).duracaoMinutos(duracao).status(status).build();
        }

        @Test
        @DisplayName("Deve retornar true quando já existe consulta no mesmo horário")
        void deveRetornarTrueQuandoHorarioOcupado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(10, 0, 30, StatusConsulta.AGENDADA)));

            boolean resultado = consultaService.verificarDisponibilidade("2025-12-01", "10:00", 5L);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consulta de 09:30 com 60 min ocupa 10:00 (sobreposição, não só horário exato)")
        void deveDetectarSobreposicao() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(9, 30, 60, StatusConsulta.AGENDADA)));

            assertThat(consultaService.verificarDisponibilidade("2025-12-01", "10:00", 5L)).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consulta CONFIRMADA também ocupa o horário")
        void confirmadaTambemOcupa() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(10, 0, 30, StatusConsulta.CONFIRMADA)));

            assertThat(consultaService.verificarDisponibilidade("2025-12-01", "10:00", 5L)).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consulta nova de 09:45 (30 min padrão) conflita com a que começa às 10:00")
        void novaQueTerminaDentroDeOutraConflita() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(10, 0, 30, StatusConsulta.AGENDADA)));

            assertThat(consultaService.verificarDisponibilidade("2025-12-01", "09:45", 5L)).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consultas encostadas (fim == início) não conflitam")
        void encostadaNaoConflita() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(9, 30, 30, StatusConsulta.AGENDADA)));

            assertThat(consultaService.verificarDisponibilidade("2025-12-01", "10:00", 5L)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando horário está disponível")
        void deveRetornarFalseQuandoHorarioDisponivel() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);

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
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
            when(consultaRepository.save(any(Consulta.class))).thenReturn(consultaAgendada);
            when(historicoRepository.save(any())).thenReturn(null);

            Consulta resultado = consultaService.atualizarConsultaByOrg(100L, requestAtualizar);

            assertThat(resultado).isNotNull();
            verify(consultaRepository).save(consultaAgendada);
        }

        @Test
        @DisplayName("Item 4 - trocar o paciente da consulta por um de outra clínica é recusado")
        void naoTrocaParaPacienteDeOutraClinica() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            lenient().when(pacienteRepository.findById(20L)).thenReturn(Optional.of(paciente));
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.empty());
            Paciente pacienteOriginal = consultaAgendada.getPaciente();

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Paciente não encontrado");
            assertThat(consultaAgendada.getPaciente()).isSameAs(pacienteOriginal);
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Trocar a forma de pagamento por uma de outra clínica é recusado")
        void naoTrocaParaFormaPagamentoDeOutraClinica() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
            when(formaPagamentoRepository.findDisponivelParaOrganizacao(99L, ORG_ID)).thenReturn(Optional.empty());
            FormaPagamento original = consultaAgendada.getFormaPagamento();
            AtualizarConsultaRequest comFormaAlheia = new AtualizarConsultaRequest(
                    5L, 20L, null, LocalDateTime.now().plusDays(2), 45, "Obs atualizada", 99L, BigDecimal.valueOf(200), null);

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, comFormaAlheia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Forma de pagamento nao encontrada");
            assertThat(consultaAgendada.getFormaPagamento()).isSameAs(original);
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Item 4 - trocar o profissional por um de outra clínica é recusado (busca filtrada pela org)")
        void naoTrocaParaProfissionalDeOutraClinica() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional não encontrado");
            verify(consultaRepository, never()).save(any());
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

        private void prepararAtualizacao() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.of(profissional));
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
        }

        @Test
        @DisplayName("Deve lançar exceção quando reagendamento sobrepõe outra consulta")
        void deveLancarExcecaoHorarioOcupadoPorOutraConsulta() {
            LocalDateTime novo = requestAtualizar.dataHora();
            Consulta outraConsulta = Consulta.builder().id(999L).organizacao(organizacao).profissional(profissional)
                    .paciente(paciente).dataHora(novo.minusMinutes(20)).duracaoMinutos(60)
                    .status(StatusConsulta.CONFIRMADA).build();
            prepararAtualizacao();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(outraConsulta));

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("consulta agendada");
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve permitir reagendar para o próprio horário (ignora a própria consulta)")
        void devePermitirReagendarParaOProprioHorario() {
            LocalDateTime novo = requestAtualizar.dataHora();
            Consulta propria = Consulta.builder().id(100L).organizacao(organizacao).profissional(profissional)
                    .paciente(paciente).dataHora(novo).duracaoMinutos(45).status(StatusConsulta.AGENDADA).build();
            prepararAtualizacao();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(propria));
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaService.atualizarConsultaByOrg(100L, requestAtualizar);

            verify(consultaRepository).save(consultaAgendada);
        }

        @Test
        @DisplayName("Deve travar o profissional antes de checar conflito no reagendamento")
        void deveTravarProfissionalNoReagendamento() {
            prepararAtualizacao();
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaService.atualizarConsultaByOrg(100L, requestAtualizar);

            org.mockito.InOrder ordem = inOrder(profissionalRepository, consultaRepository);
            ordem.verify(profissionalRepository).buscarParaAgendamentoComLock(5L, ORG_ID);
            ordem.verify(consultaRepository).findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any());
        }

        @Test
        @DisplayName("Deve bloquear edição de consulta PAGO")
        void deveBloquearEdicaoDeConsultaPaga() {
            consultaAgendada.setStatus(StatusConsulta.PAGO);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID)).thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PAGO");
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve bloquear edição de consulta CANCELADA")
        void deveBloquearEdicaoDeConsultaCancelada() {
            consultaAgendada.setStatus(StatusConsulta.CANCELADA);
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID)).thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELADA");
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar transição inválida de status na edição (AGENDADA -> REALIZADA)")
        void deveRejeitarTransicaoInvalidaNaEdicao() {
            AtualizarConsultaRequest req = new AtualizarConsultaRequest(
                    5L, 20L, null, LocalDateTime.now().plusDays(2), null, null, null, null, "REALIZADA");
            prepararAtualizacao();

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AGENDADA só pode ir para");
            assertThat(consultaAgendada.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve responder 400 (IllegalArgumentException) para status inexistente, não NPE/valueOf cru")
        void deveRejeitarStatusInvalido() {
            AtualizarConsultaRequest req = new AtualizarConsultaRequest(
                    5L, 20L, null, LocalDateTime.now().plusDays(2), null, null, null, null, "BANANA");
            prepararAtualizacao();

            assertThatThrownBy(() -> consultaService.atualizarConsultaByOrg(100L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Status inválido: BANANA");
        }

        @Test
        @DisplayName("Deve aceitar status igual ao atual (edição sem mudar status)")
        void deveAceitarStatusIgualAoAtual() {
            AtualizarConsultaRequest req = new AtualizarConsultaRequest(
                    5L, 20L, null, LocalDateTime.now().plusDays(2), null, null, null, null, "agendada");
            prepararAtualizacao();
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaService.atualizarConsultaByOrg(100L, req);

            assertThat(consultaAgendada.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
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
            when(pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(20L, ORG_ID)).thenReturn(Optional.of(paciente));
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

            List<Consulta> resultado = consultaService.buscarConsultasHoje(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas hoje")
        void deveRetornarListaVaziaQuandoSemConsultasHoje() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
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
                    eq(ORG_ID), any(), eq(dataInformada.atStartOfDay()), eq(dataInformada.atTime(23, 59, 59))))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarConsultasHoje(dataInformada);

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

            List<Consulta> resultado = consultaService.buscarConsultasDaSemanaAtual(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas na semana")
        void deveRetornarListaVaziaQuandoSemConsultasNaSemana() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
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
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
                    .thenReturn(List.of(consultaAgendada));

            List<Consulta> resultado = consultaService.buscarConsultasDoMesAtual(null);

            assertThat(resultado).hasSize(1);
            verify(consultaRepository).findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver consultas no mês")
        void deveRetornarListaVaziaQuandoSemConsultasNoMes() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByOrganizacaoIdAndPeriodo(eq(ORG_ID), any(), any(), any()))
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

    // ========== buscarHorariosOcupadosDetalhado ==========

    @Nested
    @DisplayName("buscarHorariosOcupadosDetalhado")
    class BuscarHorariosOcupadosDetalhado {

        @Test
        @DisplayName("Deve devolver início (HH:mm:ss) e duração, com 30 min quando a duração for nula")
        void deveDevolverInicioEDuracao() {
            Consulta a = Consulta.builder().id(1L).dataHora(LocalDateTime.of(2030, 5, 10, 9, 0))
                    .duracaoMinutos(60).status(StatusConsulta.AGENDADA).build();
            Consulta b = Consulta.builder().id(2L).dataHora(LocalDateTime.of(2030, 5, 10, 10, 30))
                    .duracaoMinutos(null).status(StatusConsulta.CONFIRMADA).build();
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(
                    ORG_ID, 5L, LocalDateTime.of(2030, 5, 10, 0, 0), LocalDateTime.of(2030, 5, 11, 0, 0)))
                    .thenReturn(List.of(a, b));

            List<br.com.saudeConecta.presentation.dto.consulta.HorarioOcupadoResponse> resultado =
                    consultaService.buscarHorariosOcupadosDetalhado(5L, "2030-05-10");

            assertThat(resultado).extracting("inicio", "duracaoMinutos")
                    .containsExactly(tuple("09:00:00", 60), tuple("10:30:00", 30));
        }
    }
}
