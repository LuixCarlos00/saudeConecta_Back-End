package br.com.saudeConecta.application.assinatura;

import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoProperties;
import br.com.saudeConecta.infrastructure.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.infrastructure.mercadopago.dto.PreapprovalResponse;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MercadoPagoWebhookService - Testes unitários")
class MercadoPagoWebhookServiceTest {

    private static final java.time.Clock RELOGIO = java.time.Clock.fixed(
            java.time.Instant.parse("2026-03-01T01:30:00Z"), br.com.saudeConecta.config.FusoHorarioConfig.ZONA);

    private static final String PREAPPROVAL_ID = "2c93808497f9d3a80197fa1234567890";
    private static final String AUTHORIZED_PAYMENT_ID = "9876543210";
    private static final Long ASSINATURA_ID = 42L;

    @Mock private MpWebhookEventoRepository eventoRepository;
    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private CobrancaTenantRepository cobrancaTenantRepository;
    @Mock private AssinaturaTenantService assinaturaTenantService;
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private PlatformTransactionManager transactionManager;

    private MercadoPagoWebhookService service;
    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        // Serviço real: a regra "authorized não promove status" vive nele e precisa ser exercitada aqui
        AssinaturaMercadoPagoService assinaturaMercadoPagoService = new AssinaturaMercadoPagoService(
                assinaturaTenantRepository, adminOrganizacaoRepository, mercadoPagoClient,
                new MercadoPagoProperties(), emailNotificacaoService, cobrancaTenantRepository, eventoRepository,
                mock(org.springframework.transaction.PlatformTransactionManager.class));
        service = new MercadoPagoWebhookService(eventoRepository, assinaturaTenantRepository,
                cobrancaTenantRepository, assinaturaTenantService, mercadoPagoClient,
                assinaturaMercadoPagoService, transactionManager, RELOGIO);

        Organizacao org = new Organizacao();
        org.setId(7L);

        PlanoAssinatura plano = new PlanoAssinatura();
        plano.setId(1L);
        plano.setNome("Profissional");

        assinatura = AssinaturaTenant.builder()
                .id(ASSINATURA_ID)
                .organizacao(org)
                .planoAssinatura(plano)
                .status(StatusAssinatura.TRIAL)
                .valorMensal(BigDecimal.valueOf(279))
                .mpPreapprovalId(PREAPPROVAL_ID)
                .build();

        when(eventoRepository.findByTipoAndDataId(anyString(), anyString())).thenReturn(Optional.empty());
        when(eventoRepository.save(any(MpWebhookEvento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assinaturaTenantRepository.findByMpPreapprovalId(PREAPPROVAL_ID)).thenReturn(Optional.of(assinatura));
        when(cobrancaTenantRepository.save(any(CobrancaTenant.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Não deve reprocessar evento já processado (idempotência)")
    void naoDeveReprocessarEventoJaProcessado() {
        MpWebhookEvento processado = MpWebhookEvento.builder()
                .tipo(MercadoPagoWebhookService.TOPICO_PREAPPROVAL)
                .dataId(PREAPPROVAL_ID)
                .processado(true)
                .build();
        when(eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID))
                .thenReturn(Optional.of(processado));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        verifyNoInteractions(mercadoPagoClient);
        verify(eventoRepository, never()).save(any());
    }

    @Test
    @DisplayName("2.4 - Preapproval authorized (cartão autorizado) não ativa nem renova: TRIAL segue TRIAL até a 1ª fatura paga")
    void preapprovalAuthorizedNaoAtivaNemRenova() {
        LocalDate vencimentoTrial = LocalDate.now().plusDays(5);
        assinatura.setDataVencimento(vencimentoTrial);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", "https://mp/checkout",
                        "admin@clinica.com", "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
        assertThat(assinatura.getDataVencimento()).isEqualTo(vencimentoTrial);
        verify(assinaturaTenantRepository).save(assinatura);
        verify(assinaturaTenantService, never()).renovarAposPagamento(any());
    }

    @Test
    @DisplayName("2.4 - Preapproval authorized não tira assinatura de INADIMPLENTE")
    void preapprovalAuthorizedNaoPromoveInadimplente() {
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
    }

    @Test
    @DisplayName("2.2 - Preapproval em estado não final fica pendente de reprocessamento")
    void preapprovalNaoFinalNaoMarcaProcessado() {
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(ultimoEventoSalvo().getProcessado()).isFalse();
        assertThat(ultimoEventoSalvo().getErro()).isNull();
    }

    @Test
    @DisplayName("Preapproval paused suspende e registra a data da suspensão")
    void preapprovalPausedSuspende() {
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "paused", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(assinatura.getDataSuspensao()).isNotNull();
    }

    @Test
    @DisplayName("TRIAL: preapproval cancelado pelo MP (recusa no 1º checkout) não cancela a assinatura")
    void preapprovalCancelledNoTrialNaoCancela() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null,
                        "admin@clinica.com", "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(assinatura.getMpPreapprovalId()).isNull();
        assertThat(assinatura.getMpStatus()).isNull();
        assertThat(assinatura.getCanceladoPor()).isNull();
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("Preapproval cancelled deve cancelar a assinatura registrando a origem")
    void preapprovalCancelledDeveCancelarAssinatura() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null,
                        "admin@clinica.com", "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.getCanceladoPor()).isEqualTo(OrigemCancelamento.MERCADO_PAGO);
        assertThat(assinatura.getDataCancelamento()).isNotNull();
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("Pagamento aprovado deve registrar cobrança PAGO e renovar a assinatura")
    void pagamentoAprovadoDeveRegistrarCobrancaPaga() {
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "processed",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "approved", "accredited")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        ArgumentCaptor<CobrancaTenant> captor = ArgumentCaptor.forClass(CobrancaTenant.class);
        verify(cobrancaTenantRepository).save(captor.capture());

        CobrancaTenant cobranca = captor.getValue();
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PAGO);
        assertThat(cobranca.getFormaPagamento()).isEqualTo(FormaPagamentoCobranca.MERCADO_PAGO);
        assertThat(cobranca.getMpAuthorizedPaymentId()).isEqualTo(AUTHORIZED_PAYMENT_ID);
        assertThat(cobranca.getValorTotal()).isEqualByComparingTo(BigDecimal.valueOf(279));
        // 5.5: data do pagamento vem do Clock em America/Sao_Paulo (01h30 UTC = 22h30 do dia anterior)
        assertThat(cobranca.getDataPagamento()).isEqualTo(LocalDateTime.of(2026, 2, 28, 22, 30));
        verify(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("Pagamento recusado deve registrar cobrança RECUSADO e marcar inadimplência")
    void pagamentoRecusadoDeveMarcarInadimplente() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "recycling",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "rejected", "cc_rejected_insufficient_amount")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        ArgumentCaptor<CobrancaTenant> captor = ArgumentCaptor.forClass(CobrancaTenant.class);
        verify(cobrancaTenantRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCobranca.RECUSADO);
        assertThat(captor.getValue().getMotivoRecusa()).isEqualTo("cc_rejected_insufficient_amount");
        verify(assinaturaTenantService).marcarInadimplente(ASSINATURA_ID);
        verify(assinaturaTenantService, never()).renovarAposPagamento(any());
        assertThat(ultimoEventoSalvo().getProcessado()).isFalse();
    }

    @Test
    @DisplayName("Recusa no 1º checkout com a clínica em TRIAL: registra RECUSADO e a assinatura segue TRIAL")
    void pagamentoRecusadoNoTrialNaoMarcaInadimplente() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "processed",
                        BigDecimal.valueOf(549),
                        new AuthorizedPaymentResponse.Payment(1234L, "rejected", "cc_rejected_other_reason")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        ArgumentCaptor<CobrancaTenant> captor = ArgumentCaptor.forClass(CobrancaTenant.class);
        verify(cobrancaTenantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCobranca.RECUSADO);
        assertThat(captor.getValue().getMotivoRecusa()).isEqualTo("cc_rejected_other_reason");
        verify(assinaturaTenantService, never()).marcarInadimplente(any());
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
    }

    @Test
    @DisplayName("Pagamento recusado com a assinatura já INADIMPLENTE não chama a marcação de novo")
    void pagamentoRecusadoEmInadimplenteNaoMarcaDeNovo() {
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "recycling",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "rejected", "cc_rejected_other_reason")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService, never()).marcarInadimplente(any());
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
    }

    @Test
    @DisplayName("Pagamento recusado não rebaixa assinatura SUSPENSA para INADIMPLENTE")
    void pagamentoRecusadoNaoRebaixaSuspensa() {
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "recycling",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "rejected", "cc_rejected_other_reason")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService, never()).marcarInadimplente(any());
    }

    @Test
    @DisplayName("2.2 - Fatura sem pagamento processado não marca o evento como processado")
    void faturaSemPagamentoNaoMarcaProcessado() {
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "scheduled",
                        BigDecimal.valueOf(279), null));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        assertThat(ultimoEventoSalvo().getProcessado()).isFalse();
        verifyNoInteractions(assinaturaTenantService);
    }

    @Test
    @DisplayName("2.2 - Evento já registrado mas não final é reprocessado quando chega nova notificação")
    void eventoNaoFinalEhReprocessado() {
        MpWebhookEvento pendente = MpWebhookEvento.builder()
                .id(5L)
                .tipo(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT)
                .dataId(AUTHORIZED_PAYMENT_ID)
                .processado(false)
                .recebidoEm(LocalDateTime.now().minusDays(1))
                .build();
        when(eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT,
                AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.of(pendente));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "processed",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "approved", "accredited")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("2.9 - Tipo forjado: recurso consultado com id diferente do data.id é ignorado")
    void tipoForjadoComRecursoDivergenteEhIgnorado() {
        when(mercadoPagoClient.buscarAuthorizedPayment(PREAPPROVAL_ID)).thenReturn(
                new AuthorizedPaymentResponse("outro-id", PREAPPROVAL_ID, "processed",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "approved", "accredited")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, PREAPPROVAL_ID, "{}");

        verifyNoInteractions(assinaturaTenantService);
        verify(cobrancaTenantRepository, never()).save(any());
        assertThat(ultimoEventoSalvo().getProcessado()).isFalse();
    }

    @Test
    @DisplayName("2.9 - Tipo forjado: preapproval consultado com id diferente do data.id é ignorado")
    void preapprovalComIdDivergenteEhIgnorado() {
        when(mercadoPagoClient.buscarPreapproval(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, AUTHORIZED_PAYMENT_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        verify(assinaturaTenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pagamento já registrado como pago não deve renovar novamente")
    void pagamentoDuplicadoNaoDeveRenovarNovamente() {
        CobrancaTenant jaPaga = CobrancaTenant.builder()
                .id(99L)
                .assinaturaTenant(assinatura)
                .organizacao(assinatura.getOrganizacao())
                .valorTotal(BigDecimal.valueOf(279))
                .status(StatusCobranca.PAGO)
                .mpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)
                .build();
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID))
                .thenReturn(Optional.of(jaPaga));
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, PREAPPROVAL_ID, "processed",
                        BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "approved", "accredited")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService, never()).renovarAposPagamento(any());
        verify(cobrancaTenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tópico desconhecido deve ser ignorado sem erro")
    void topicoDesconhecidoDeveSerIgnorado() {
        service.processar("payment", "1", "{}");

        verifyNoInteractions(mercadoPagoClient);

        ArgumentCaptor<MpWebhookEvento> captor = ArgumentCaptor.forClass(MpWebhookEvento.class);
        verify(eventoRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getProcessado()).isTrue();
        assertThat(captor.getValue().getErro()).isNull();
    }

    // ========== Pós-revisão: H1, H3, M1, M2, M3, L3 ==========

    private AuthorizedPaymentResponse faturaAprovada(String id) {
        return new AuthorizedPaymentResponse(id, PREAPPROVAL_ID, "processed", BigDecimal.valueOf(279),
                new AuthorizedPaymentResponse.Payment(1234L, "approved", "accredited"));
    }

    @Test
    @DisplayName("M3 - Falha no GET da fatura grava o erro por UPDATE condicional, sem salvar a cópia desanexada")
    void falhaGravaErroPorUpdateCondicional() {
        MpWebhookEvento aberto = MpWebhookEvento.builder().id(5L)
                .tipo(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT).dataId(AUTHORIZED_PAYMENT_ID)
                .processado(false).tentativas(2).recebidoEm(LocalDateTime.now()).build();
        when(eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT,
                AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.of(aberto));
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID))
                .thenThrow(new RuntimeException("502 Bad Gateway"));

        LocalDateTime antes = LocalDateTime.now(RELOGIO);
        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        ArgumentCaptor<LocalDateTime> proxima = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(eventoRepository).registrarFalha(eq(5L), eq("502 Bad Gateway"), proxima.capture());
        // backoff cresce com as tentativas já feitas (2 falhas anteriores -> 20 min)
        assertThat(proxima.getValue()).isAfterOrEqualTo(antes.plusMinutes(20));
        verify(eventoRepository, never()).save(any());
    }

    @Test
    @DisplayName("H3 - Conflito de versão no webhook: re-lê e reaplica (até 3x) sem perder a renovação")
    void conflitoDeVersaoNoWebhookReaplica() {
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));
        doThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(AssinaturaTenant.class, ASSINATURA_ID))
                .doNothing()
                .when(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService, times(2)).renovarAposPagamento(ASSINATURA_ID);
        verify(eventoRepository, never()).registrarFalha(any(), any(), any());
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("H3 - Conflito de versão persistente: desiste após 3 tentativas e registra o erro para o job")
    void conflitoDeVersaoPersistenteRegistraErro() {
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));
        doThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(AssinaturaTenant.class, ASSINATURA_ID))
                .when(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService, times(3)).renovarAposPagamento(ASSINATURA_ID);
        verify(eventoRepository).registrarFalha(any(), anyString(), any());
    }

    @Test
    @DisplayName("M2 - Pagamento aprovado em assinatura CANCELADA registra PAGO, não reativa e encerra o evento")
    void pagamentoEmCanceladaNaoReativa() {
        assinatura.setStatus(StatusAssinatura.CANCELADA);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        ArgumentCaptor<CobrancaTenant> captor = ArgumentCaptor.forClass(CobrancaTenant.class);
        verify(cobrancaTenantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCobranca.PAGO);
        verify(assinaturaTenantService, never()).renovarAposPagamento(any());
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("M1 - Pagamento aprovado com preapproval pausado: renova e reativa o preapproval no MP")
    void pagamentoComPreapprovalPausadoReativa() {
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setMpStatus("paused");
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));
        when(mercadoPagoClient.reativarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", null, null, null, null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        verify(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);
        verify(mercadoPagoClient).reativarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
    }

    @Test
    @DisplayName("H1b - Recupera faturas aprovadas do preapproval sem cobrança PAGO local; ignora pagas e não aprovadas")
    void recuperaFaturasAprovadasSemRegistro() {
        CobrancaTenant jaPaga = CobrancaTenant.builder().status(StatusCobranca.PAGO).mpAuthorizedPaymentId("f-paga").build();
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID)).thenReturn(java.util.List.of(
                faturaAprovada("f-paga"),
                new AuthorizedPaymentResponse("f-recusada", PREAPPROVAL_ID, "recycling", BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1L, "rejected", "cc_rejected_other_reason")),
                faturaAprovada("f-perdida")));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("f-paga")).thenReturn(Optional.of(jaPaga));
        CobrancaTenant[] registrada = new CobrancaTenant[1];
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("f-perdida"))
                .thenAnswer(inv -> Optional.ofNullable(registrada[0]));
        when(cobrancaTenantRepository.save(any(CobrancaTenant.class))).thenAnswer(inv -> {
            registrada[0] = inv.getArgument(0);
            return registrada[0];
        });
        when(mercadoPagoClient.buscarAuthorizedPayment("f-perdida")).thenReturn(faturaAprovada("f-perdida"));

        int recuperadas = service.recuperarFaturasAprovadas(PREAPPROVAL_ID);

        assertThat(recuperadas).isEqualTo(1);
        verify(mercadoPagoClient, never()).buscarAuthorizedPayment("f-paga");
        verify(mercadoPagoClient, never()).buscarAuthorizedPayment("f-recusada");
        verify(assinaturaTenantService, times(1)).renovarAposPagamento(ASSINATURA_ID);
        assertThat(registrada[0].getStatus()).isEqualTo(StatusCobranca.PAGO);
    }

    @Test
    @DisplayName("H1a/L3 - Job expira eventos antigos com erro e reprocessa os elegíveis pelo backoff")
    void jobExpiraAntigosEReprocessaElegiveis() {
        MpWebhookEvento comErro = MpWebhookEvento.builder().id(9L)
                .tipo(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT).dataId(AUTHORIZED_PAYMENT_ID)
                .payload("{}").processado(false).erro("timeout").tentativas(1)
                .recebidoEm(LocalDateTime.now().minusHours(1)).build();
        when(eventoRepository.expirarComErroAntesDe(any(), any())).thenReturn(2);
        when(eventoRepository.findComErroParaReprocessar(any(), any())).thenReturn(java.util.List.of(comErro));
        when(eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT,
                AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.of(comErro));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));

        LocalDateTime antes = LocalDateTime.now(RELOGIO);
        int reprocessados = service.reprocessarEventosComErro();

        assertThat(reprocessados).isEqualTo(1);
        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(eventoRepository).expirarComErroAntesDe(limite.capture(), any());
        assertThat(limite.getValue()).isBefore(antes.minusDays(3).plusMinutes(1)).isAfter(antes.minusDays(3).minusMinutes(1));
        verify(eventoRepository).findComErroParaReprocessar(eq(limite.getValue()), any());
        verify(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);
        assertThat(comErro.getProcessado()).isTrue();
        assertThat(comErro.getErro()).isNull();
    }

    @Test
    @DisplayName("N5 - Falha inesperada em um evento do lote não impede o reprocessamento dos demais")
    void falhaEmUmEventoNaoDerrubaOLote() {
        MpWebhookEvento quebrado = MpWebhookEvento.builder().id(8L)
                .tipo(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT).dataId("fatura-quebrada")
                .payload("{}").processado(false).erro("timeout").tentativas(1)
                .recebidoEm(LocalDateTime.now().minusHours(1)).build();
        MpWebhookEvento bom = MpWebhookEvento.builder().id(9L)
                .tipo(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT).dataId(AUTHORIZED_PAYMENT_ID)
                .payload("{}").processado(false).erro("timeout").tentativas(1)
                .recebidoEm(LocalDateTime.now().minusHours(1)).build();
        when(eventoRepository.findComErroParaReprocessar(any(), any())).thenReturn(java.util.List.of(quebrado, bom));
        when(eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, "fatura-quebrada"))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("conexão perdida"));
        when(eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT,
                AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.of(bom));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));

        int reprocessados = service.reprocessarEventosComErro();

        assertThat(reprocessados).isEqualTo(1);
        verify(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);
        assertThat(bom.getProcessado()).isTrue();
    }

    @Test
    @DisplayName("N7 - Pagamento aprovado em assinatura CANCELADA marca o evento para tratamento manual")
    void pagamentoEmCanceladaMarcaEventoParaAtencao() {
        assinatura.setStatus(StatusAssinatura.CANCELADA);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId(AUTHORIZED_PAYMENT_ID)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(faturaAprovada(AUTHORIZED_PAYMENT_ID));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        MpWebhookEvento salvo = ultimoEventoSalvo();
        assertThat(salvo.getProcessado()).isTrue();
        assertThat(salvo.getRequerAtencao()).isTrue();
        assertThat(salvo.getMotivoAtencao()).startsWith(MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA);
        verify(assinaturaTenantService, never()).renovarAposPagamento(any());
    }

    @Test
    @DisplayName("Preapproval antigo cancelado não altera a assinatura já vinculada a outro preapproval")
    void preapprovalAntigoCanceladoNaoAlteraAssinaturaVinculadaAOutro() {
        String antigo = "preapproval-antigo";
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        assinatura.setMpStatus("pending");
        when(assinaturaTenantRepository.findByMpPreapprovalId(antigo)).thenReturn(Optional.empty());
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarPreapproval(antigo)).thenReturn(
                new PreapprovalResponse(antigo, "cancelled", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, antigo, "{}");

        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getMpStatus()).isEqualTo("pending");
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(assinatura.getCanceladoPor()).isNull();
        verify(assinaturaTenantRepository, never()).save(any());
        // Encerrado: não há nada a reaplicar para um preapproval que não é mais o vinculado
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
        assertThat(ultimoEventoSalvo().getErro()).isNull();
    }

    @Test
    @DisplayName("A2 - Fatura aprovada de assinatura em cortesia: registra PAGO, não renova nem reativa e pede atenção")
    void pagamentoEmCortesiaNaoRenova() {
        assinatura.setCortesia(true);
        assinatura.setMpStatus("paused");
        when(assinaturaTenantRepository.findByMpPreapprovalId(PREAPPROVAL_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarAuthorizedPayment("fat-cortesia")).thenReturn(new AuthorizedPaymentResponse(
                "fat-cortesia", PREAPPROVAL_ID, "processed", java.math.BigDecimal.TEN,
                new AuthorizedPaymentResponse.Payment(1L, "approved", "accredited")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, "fat-cortesia", "{}");

        verify(assinaturaTenantService, never()).renovarAposPagamento(any());
        verify(mercadoPagoClient, never()).reativarPreapproval(any());
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
        assertThat(ultimoEventoSalvo().getMotivoAtencao()).startsWith(MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CORTESIA);
    }

    @Test
    @DisplayName("C3 - Webhook cancelled do preapproval substituído (ainda pendente de limpeza) é ignorado")
    void preapprovalSubstituidoCanceladoEhIgnorado() {
        String antigo = "preapproval-antigo";
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setMpPreapprovalId(null);
        assinatura.setMpStatus(null);
        assinatura.setMpPreapprovalSubstituidoId(antigo);
        when(assinaturaTenantRepository.findByMpPreapprovalId(antigo)).thenReturn(Optional.empty());
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarPreapproval(antigo)).thenReturn(
                new PreapprovalResponse(antigo, "cancelled", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, antigo, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(assinatura.getMpPreapprovalId()).isNull();
        verify(assinaturaTenantRepository, never()).save(any());
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("C3 - Cancelado achado pela external_reference sem preapproval vinculado não cancela a assinatura")
    void canceladoSemVinculoNaoCancelaAssinatura() {
        String antigo = "preapproval-antigo";
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setMpPreapprovalId(null);
        when(assinaturaTenantRepository.findByMpPreapprovalId(antigo)).thenReturn(Optional.empty());
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarPreapproval(antigo)).thenReturn(
                new PreapprovalResponse(antigo, "cancelled", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, antigo, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(assinatura.getMpPreapprovalId()).isNull();
        verify(assinaturaTenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Preapproval novo cujo vínculo ainda não foi commitado é ignorado sem encerrar o evento")
    void preapprovalNovoAntesDoVinculoFicaAbertoParaReentrega() {
        String novo = "preapproval-novo";
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        assinatura.setMpStatus("pending");
        when(assinaturaTenantRepository.findByMpPreapprovalId(novo)).thenReturn(Optional.empty());
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarPreapproval(novo)).thenReturn(
                new PreapprovalResponse(novo, "authorized", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, novo, "{}");

        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        verify(assinaturaTenantRepository, never()).save(any());
        // Aberto: a próxima entrega, já com o vínculo novo commitado, aplica o status
        assertThat(ultimoEventoSalvo().getProcessado()).isFalse();
        assertThat(ultimoEventoSalvo().getErro()).isNull();
    }

    @Test
    @DisplayName("Primeiro vínculo: assinatura sem preapproval é achada pela external_reference e vinculada")
    void primeiroVinculoPelaExternalReferenceContinuaFuncionando() {
        String novo = "preapproval-novo";
        assinatura.setMpPreapprovalId(null);
        when(assinaturaTenantRepository.findByMpPreapprovalId(novo)).thenReturn(Optional.empty());
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarPreapproval(novo)).thenReturn(
                new PreapprovalResponse(novo, "authorized", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, novo, "{}");

        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(novo);
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
        verify(assinaturaTenantRepository).save(assinatura);
    }

    @Test
    @DisplayName("Mesmo preapproval achado pela external_reference continua aplicando o status")
    void mesmoPreapprovalPeloFallbackAplicaStatus() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        when(assinaturaTenantRepository.findByMpPreapprovalId(PREAPPROVAL_ID)).thenReturn(Optional.empty());
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null, null, "ASSINATURA-42", null, null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(ultimoEventoSalvo().getProcessado()).isTrue();
    }

    @Test
    @DisplayName("Fatura aprovada de preapproval sem assinatura vinculada vira pendência manual, sem mexer em outra assinatura")
    void faturaAprovadaDePreapprovalAntigoViraPendenciaManual() {
        String antigo = "preapproval-antigo";
        when(assinaturaTenantRepository.findByMpPreapprovalId(antigo)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, antigo, "processed", BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "approved", "accredited")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        MpWebhookEvento salvo = ultimoEventoSalvo();
        assertThat(salvo.getRequerAtencao()).isTrue();
        assertThat(salvo.getMotivoAtencao()).startsWith(MpWebhookEvento.MOTIVO_PAGAMENTO_SEM_ASSINATURA);
        assertThat(salvo.getProcessado()).isFalse();
        verify(cobrancaTenantRepository, never()).save(any());
        verifyNoInteractions(assinaturaTenantService);
    }

    @Test
    @DisplayName("Fatura não aprovada de preapproval sem assinatura vinculada não vira pendência")
    void faturaRecusadaDePreapprovalAntigoNaoViraPendencia() {
        String antigo = "preapproval-antigo";
        when(assinaturaTenantRepository.findByMpPreapprovalId(antigo)).thenReturn(Optional.empty());
        when(mercadoPagoClient.buscarAuthorizedPayment(AUTHORIZED_PAYMENT_ID)).thenReturn(
                new AuthorizedPaymentResponse(AUTHORIZED_PAYMENT_ID, antigo, "processed", BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1234L, "rejected", "cc_rejected_other_reason")));

        service.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, AUTHORIZED_PAYMENT_ID, "{}");

        assertThat(ultimoEventoSalvo().getRequerAtencao()).isFalse();
        verify(cobrancaTenantRepository, never()).save(any());
        verifyNoInteractions(assinaturaTenantService);
    }

    private MpWebhookEvento ultimoEventoSalvo() {
        ArgumentCaptor<MpWebhookEvento> captor = ArgumentCaptor.forClass(MpWebhookEvento.class);
        verify(eventoRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
