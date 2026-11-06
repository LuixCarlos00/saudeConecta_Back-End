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
@DisplayName("ConsultaAgendamentoService - cadastro, edição, status, exclusão e agenda")
class ConsultaAgendamentoServiceTest {

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
    private ConsultaAgendamentoService consultaAgendamentoService;

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

            Consulta resultado = consultaAgendamentoService.cadastrarConsultaByOrg(requestValido);

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

            consultaAgendamentoService.cadastrarConsultaByOrg(requestSemDuracao);
        }

        @Test
        @DisplayName("Deve lançar exceção quando organização não for encontrada")
        void deveLancarExcecaoQuandoOrganizacaoNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requestValido))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Organização não encontrada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não for encontrado")
        void deveLancarExcecaoQuandoProfissionalNaoEncontrado() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(organizacaoRepository.findById(ORG_ID)).thenReturn(Optional.of(organizacao));
            when(profissionalRepository.buscarClinicoIdByOrg(5L, ORG_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requestValido))
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

            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requestValido))
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

            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requestValido))
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

            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(comFormaAlheia))
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

            var requisicao = requestEm(nove.plusMinutes(30), 30);
            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requisicao))
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

            var requisicao = requestEm(nove, 30);
            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requisicao))
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

            var requisicao = requestEm(nove, 60);
            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requisicao))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Deve usar 30 min quando a consulta existente não tem duração")
        void deveUsarFallbackDeDuracaoDaExistente() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(existente(nove, null, StatusConsulta.AGENDADA)));

            var requisicao = requestEm(nove.plusMinutes(29), 30);
            assertThatThrownBy(() -> consultaAgendamentoService.cadastrarConsultaByOrg(requisicao))
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

            consultaAgendamentoService.cadastrarConsultaByOrg(requestEm(nove.plusMinutes(60), 30));

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

            consultaAgendamentoService.cadastrarConsultaByOrg(requestEm(nove.minusMinutes(30), 30));

            verify(consultaRepository).save(any(Consulta.class));
        }

        @Test
        @DisplayName("Deve travar o profissional (lock pessimista) antes de checar conflito e salvar")
        void deveTravarProfissionalAntesDeSalvar() {
            LocalDateTime nove = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            prepararCadastro();
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaAgendamentoService.cadastrarConsultaByOrg(requestEm(nove, 30));

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

            consultaAgendamentoService.cadastrarConsultaByOrg(requestComEspecialidade);

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

            Consulta resultado = consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.CONFIRMADA, null);
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("AGENDADA → CANCELADA deve exigir motivo")
        void deveLancarExcecaoAoCancelarSemMotivo() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.CANCELADA, null))
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

            consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.CANCELADA, "Paciente cancelou");
            assertThat(consultaAgendada.getMotivoCancelamento()).isEqualTo("Paciente cancelou");
        }

        @Test
        @DisplayName("AGENDADA → REALIZADA deve ser bloqueado")
        void deveBloquerarTransicaoAgendadaParaRealizada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.REALIZADA, null))
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

            Consulta resultado = consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.AGENDADA, null);
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("CONFIRMADA → PAGO deve ser bloqueado")
        void deveBloquerarTransicaoConfirmadaParaPago() {
            consultaAgendada.setStatus(StatusConsulta.CONFIRMADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.PAGO, null))
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

            Consulta resultado = consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.PAGO, null);
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("REALIZADA → AGENDADA deve ser bloqueado")
        void deveBloquerarTransicaoRealizadaParaAgendada() {
            consultaAgendada.setStatus(StatusConsulta.REALIZADA);

            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(100L, ORG_ID))
                    .thenReturn(Optional.of(consultaAgendada));

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(100L, StatusConsulta.AGENDADA, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não podem ter o status alterado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for encontrada")
        void deveLancarExcecaoQuandoConsultaNaoEncontrada() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findByIdAndOrganizacao_Id(999L, ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarStatus(999L, StatusConsulta.CONFIRMADA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consulta não encontrada com ID: 999");
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

            boolean resultado = consultaAgendamentoService.verificarDisponibilidade("2025-12-01", "10:00", 5L);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consulta de 09:30 com 60 min ocupa 10:00 (sobreposição, não só horário exato)")
        void deveDetectarSobreposicao() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(9, 30, 60, StatusConsulta.AGENDADA)));

            assertThat(consultaAgendamentoService.verificarDisponibilidade("2025-12-01", "10:00", 5L)).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consulta CONFIRMADA também ocupa o horário")
        void confirmadaTambemOcupa() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(10, 0, 30, StatusConsulta.CONFIRMADA)));

            assertThat(consultaAgendamentoService.verificarDisponibilidade("2025-12-01", "10:00", 5L)).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consulta nova de 09:45 (30 min padrão) conflita com a que começa às 10:00")
        void novaQueTerminaDentroDeOutraConflita() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(10, 0, 30, StatusConsulta.AGENDADA)));

            assertThat(consultaAgendamentoService.verificarDisponibilidade("2025-12-01", "09:45", 5L)).isTrue();
        }

        @Test
        @DisplayName("Fase 5 - consultas encostadas (fim == início) não conflitam")
        void encostadaNaoConflita() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findAtivasDoProfissionalNoPeriodo(eq(ORG_ID), eq(5L), any(), any()))
                    .thenReturn(List.of(consultaAs(9, 30, 30, StatusConsulta.AGENDADA)));

            assertThat(consultaAgendamentoService.verificarDisponibilidade("2025-12-01", "10:00", 5L)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando horário está disponível")
        void deveRetornarFalseQuandoHorarioDisponivel() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);

            boolean resultado = consultaAgendamentoService.verificarDisponibilidade("2025-12-01", "10:00", 5L);

            assertThat(resultado).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando data/hora for inválida")
        void deveRetornarFalseQuandoDataHoraInvalida() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);

            boolean resultado = consultaAgendamentoService.verificarDisponibilidade("data-invalida", "hora-invalida", 5L);

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

            consultaAgendamentoService.deletarConsulta(100L);

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

            assertThatThrownBy(() -> consultaAgendamentoService.deletarConsulta(999L))
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

            assertThatThrownBy(() -> consultaAgendamentoService.deletarConsulta(100L))
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
            br.com.saudeConecta.domain.prontuario.Prontuario prontuarioMedico =
                    mock(br.com.saudeConecta.domain.prontuario.Prontuario.class);
            when(prontuarioRepository.findByConsulta_Id(100L))
                    .thenReturn(prontuarioMedico);

            assertThatThrownBy(() -> consultaAgendamentoService.deletarConsulta(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prontuário médico");
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

            Consulta resultado = consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar);

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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, comFormaAlheia))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Profissional não encontrado");
            verify(consultaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando consulta não for encontrada")
        void deveLancarExcecaoConsultaNaoEncontrada() {
            when(consultaRepository.findByIdAndOrganizacao_Id(anyLong(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(999L, requestAtualizar))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar))
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

            consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar);

            verify(consultaRepository).save(consultaAgendada);
        }

        @Test
        @DisplayName("Deve travar o profissional antes de checar conflito no reagendamento")
        void deveTravarProfissionalNoReagendamento() {
            prepararAtualizacao();
            when(consultaRepository.save(any())).thenReturn(consultaAgendada);

            consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar);

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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, requestAtualizar))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, req))
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

            assertThatThrownBy(() -> consultaAgendamentoService.atualizarConsultaByOrg(100L, req))
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

            consultaAgendamentoService.atualizarConsultaByOrg(100L, req);

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

            consultaAgendamentoService.atualizarConsultaByOrg(100L, requestComStatus);

            assertThat(consultaAgendada.getStatus()).isEqualTo(StatusConsulta.CONFIRMADA);
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

            List<String> resultado = consultaAgendamentoService.buscarHorariosOcupados(5L, "2024-06-15");

            assertThat(resultado).containsExactly("08:00", "09:00", "14:00");
            verify(consultaRepository).findHorariosOcupados(eq(ORG_ID), eq(5L), any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando profissional não tem consultas na data")
        void deveRetornarVazioQuandoSemHorarios() {
            when(tenantHelper.getCurrentTenantId()).thenReturn(ORG_ID);
            when(consultaRepository.findHorariosOcupados(eq(ORG_ID), eq(5L), any()))
                    .thenReturn(Collections.emptyList());

            List<String> resultado = consultaAgendamentoService.buscarHorariosOcupados(5L, "2024-06-15");

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

            List<br.com.saudeConecta.application.consulta.dto.HorarioOcupadoResponse> resultado =
                    consultaAgendamentoService.buscarHorariosOcupadosDetalhado(5L, "2030-05-10");

            assertThat(resultado).extracting("inicio", "duracaoMinutos")
                    .containsExactly(tuple("09:00:00", 60), tuple("10:30:00", 30));
        }
    }

    // ========== caches limpos pela escrita ==========

    @Nested
    @DisplayName("Caches limpos pelas escritas")
    class CachesLimpos {

        private java.util.Set<String> cachesLimpos(String metodo) {
            java.lang.reflect.Method m = java.util.Arrays.stream(ConsultaAgendamentoService.class.getDeclaredMethods())
                    .filter(x -> x.getName().equals(metodo))
                    .findFirst().orElseThrow();
            org.springframework.cache.annotation.CacheEvict evict =
                    m.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertThat(evict.allEntries()).isTrue();
            return java.util.Set.of(evict.value());
        }

        @Test
        @DisplayName("Cadastrar limpa o dashboard do profissional (antes ficava velho até expirar)")
        void cadastrarLimpaDashboardProfissional() {
            assertThat(cachesLimpos("cadastrarConsultaByOrg")).contains("dashboard-profissional");
        }

        @Test
        @DisplayName("Cadastrar, atualizar, status e excluir limpam os mesmos caches")
        void escritasLimpamOsMesmosCaches() {
            java.util.Set<String> atualizar = cachesLimpos("atualizarConsultaByOrg");

            assertThat(cachesLimpos("cadastrarConsultaByOrg")).isEqualTo(atualizar);
            assertThat(cachesLimpos("atualizarStatus")).isEqualTo(atualizar);
            assertThat(cachesLimpos("deletarConsulta")).isEqualTo(atualizar);
        }
    }
}
