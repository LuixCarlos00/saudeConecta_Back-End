package br.com.saudeConecta.e2e;

import br.com.saudeConecta.application.assinatura.AssinaturaTenantService;
import br.com.saudeConecta.application.assinatura.MercadoPagoWebhookService;
import br.com.saudeConecta.application.cobranca.CobrancaScheduler;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.infrastructure.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.infrastructure.mercadopago.dto.PreapprovalResponse;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanoAssinaturaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponta a ponta da cobrança SaaS com banco real (H2) e transações reais.
 * Só o transporte HTTP do Mercado Pago (MercadoPagoClient) e o e-mail são simulados.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("E2E - Cobrança Mercado Pago: webhooks, reconciliação e régua")
class CobrancaMercadoPagoFluxoE2ETest {

    private static final String PREAPPROVAL_ID = "pa-e2e-0001";
    private static final String FATURA_ID = "777000111";

    @MockBean private MercadoPagoClient mercadoPagoClient;
    @MockBean private EmailNotificacaoService emailNotificacaoService;

    @Autowired private MercadoPagoWebhookService webhookService;
    @Autowired private CobrancaScheduler cobrancaScheduler;
    @Autowired private AssinaturaTenantRepository assinaturaTenantRepository;
    @Autowired private CobrancaTenantRepository cobrancaTenantRepository;
    @Autowired private MpWebhookEventoRepository eventoRepository;
    @Autowired private OrganizacaoRepository organizacaoRepository;
    @Autowired private PlanoAssinaturaRepository planoAssinaturaRepository;
    @Autowired private AssinaturaTenantService assinaturaTenantService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mockMvc;

    private Organizacao organizacao;
    private PlanoAssinatura plano;

    @BeforeEach
    void setUp() {
        organizacao = new Organizacao();
        organizacao.setNome("Clínica E2E Cobrança");
        organizacao.setTipo(TipoOrganizacao.CLINICA);
        organizacao = organizacaoRepository.save(organizacao);

        plano = new PlanoAssinatura();
        plano.setNome("Profissional E2E");
        plano.setTipo(TipoPlano.PROFISSIONAL);
        plano.setValorMensal(BigDecimal.valueOf(279));
        plano.setAtivo(true);
        plano = planoAssinaturaRepository.save(plano);
    }

    @AfterEach
    void limpar() {
        cobrancaTenantRepository.deleteAll(cobrancaTenantRepository.findAll().stream()
                .filter(c -> c.getOrganizacao().getId().equals(organizacao.getId())).toList());
        eventoRepository.deleteAll();
        assinaturaTenantRepository.deleteAll(assinaturaTenantRepository.findByOrganizacaoId(organizacao.getId()));
        organizacaoRepository.delete(organizacao);
        planoAssinaturaRepository.delete(plano);
    }

    private AssinaturaTenant salvarAssinatura(StatusAssinatura status, String preapprovalId, String mpStatus,
                                              LocalDate vencimento) {
        return assinaturaTenantRepository.save(AssinaturaTenant.builder()
                .organizacao(organizacao)
                .planoAssinatura(plano)
                .status(status)
                .dataInicio(LocalDate.now().minusMonths(2))
                .dataVencimento(vencimento)
                .dataProximaCobranca(vencimento)
                .valorMensal(BigDecimal.valueOf(279))
                .qtdAdminExtra(0)
                .qtdProfissionalExtra(0)
                .qtdSecretariaExtra(0)
                .valorAdicionalPerfis(BigDecimal.ZERO)
                .mpPreapprovalId(preapprovalId)
                .mpStatus(mpStatus)
                .build());
    }

    private AuthorizedPaymentResponse fatura(String statusPagamento) {
        AuthorizedPaymentResponse.Payment pagamento = statusPagamento == null ? null
                : new AuthorizedPaymentResponse.Payment(55L, statusPagamento,
                "approved".equals(statusPagamento) ? "accredited" : "cc_rejected_insufficient_amount");
        return new AuthorizedPaymentResponse(FATURA_ID, PREAPPROVAL_ID,
                statusPagamento == null ? "scheduled" : "processed", BigDecimal.valueOf(279), pagamento);
    }

    private AssinaturaTenant recarregar(AssinaturaTenant assinatura) {
        return assinaturaTenantRepository.findById(assinatura.getId()).orElseThrow();
    }

    private MpWebhookEvento eventoDaFatura() {
        return eventoRepository.findByTipoAndDataId(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID)
                .orElseThrow();
    }

    @Test
    @DisplayName("Fatura: sem pagamento → recusada → aprovada termina PAGA e renovada; aprovado repetido não renova 2x")
    void sequenciaDeWebhooksDaFatura() {
        LocalDate vencimentoOriginal = LocalDate.now();
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.ATIVA, PREAPPROVAL_ID, "authorized",
                vencimentoOriginal);
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID))
                .thenReturn(fatura(null), fatura("rejected"), fatura("approved"), fatura("approved"));

        // 1) Fatura criada, ainda sem pagamento: nada muda e o evento continua aberto
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");
        assertThat(cobrancaTenantRepository.findByMpAuthorizedPaymentId(FATURA_ID)).isEmpty();
        assertThat(eventoDaFatura().getProcessado()).isFalse();
        assertThat(recarregar(assinatura).getStatus()).isEqualTo(StatusAssinatura.ATIVA);

        // 2) Cartão recusado: cobrança RECUSADO, assinatura INADIMPLENTE, evento ainda aberto (MP vai tentar de novo)
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");
        assertThat(cobrancaTenantRepository.findByMpAuthorizedPaymentId(FATURA_ID).orElseThrow().getStatus())
                .isEqualTo(StatusCobranca.RECUSADO);
        assertThat(recarregar(assinatura).getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(eventoDaFatura().getProcessado()).isFalse();

        // 3) Nova tentativa aprovada: cobrança PAGA, assinatura ATIVA e vencimento renovado
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");
        CobrancaTenant cobranca = cobrancaTenantRepository.findByMpAuthorizedPaymentId(FATURA_ID).orElseThrow();
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PAGO);
        assertThat(cobranca.getDataPagamento()).isNotNull();
        AssinaturaTenant renovada = recarregar(assinatura);
        assertThat(renovada.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(renovada.getDataVencimento()).isAfter(vencimentoOriginal);
        assertThat(eventoDaFatura().getProcessado()).isTrue();
        LocalDate vencimentoRenovado = renovada.getDataVencimento();

        // 4) Reentrega do mesmo evento já final: ignorada sem consultar o MP
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");
        verify(mercadoPagoClient, times(3)).buscarAuthorizedPayment(FATURA_ID);

        // 5) Mesmo que o evento seja reaberto, cobranca.isPago() impede a segunda renovação
        MpWebhookEvento evento = eventoDaFatura();
        evento.setProcessado(false);
        eventoRepository.save(evento);
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");
        assertThat(recarregar(assinatura).getDataVencimento()).isEqualTo(vencimentoRenovado);
        assertThat(cobrancaTenantRepository.findAll().stream()
                .filter(c -> FATURA_ID.equals(c.getMpAuthorizedPaymentId())).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Reconciliação após inadimplência/suspensão não volta para ATIVA quando o MP diz authorized")
    void reconciliacaoNaoDesfazInadimplencia() {
        LocalDate vencimento = LocalDate.now().minusDays(8);
        AssinaturaTenant inadimplente = salvarAssinatura(StatusAssinatura.INADIMPLENTE, PREAPPROVAL_ID,
                "authorized", vencimento);
        LocalDateTime suspensaEm = LocalDateTime.now().minusDays(4).withNano(0);
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, "pa-e2e-0002", "authorized",
                LocalDate.now().minusDays(20));
        suspensa.setDataSuspensao(suspensaEm);
        assinaturaTenantRepository.save(suspensa);
        when(mercadoPagoClient.buscarPreapproval(anyString())).thenAnswer(inv -> new PreapprovalResponse(
                inv.getArgument(0), "authorized", null, null, null, null, null));

        cobrancaScheduler.reconciliarComMercadoPago();

        AssinaturaTenant inadimplenteDepois = recarregar(inadimplente);
        assertThat(inadimplenteDepois.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(inadimplenteDepois.getDataVencimento()).isEqualTo(vencimento);
        AssinaturaTenant suspensaDepois = recarregar(suspensa);
        assertThat(suspensaDepois.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(suspensaDepois.getDataSuspensao()).isEqualTo(suspensaEm);
    }

    @Test
    @DisplayName("Preapproval authorized via webhook não ativa o TRIAL nem estende o vencimento")
    void webhookAuthorizedMantemTrial() {
        LocalDate fimDoTrial = LocalDate.now().plusDays(6);
        AssinaturaTenant trial = salvarAssinatura(StatusAssinatura.TRIAL, PREAPPROVAL_ID, "pending", fimDoTrial);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(new PreapprovalResponse(
                PREAPPROVAL_ID, "authorized", null, null, "ASSINATURA-" + trial.getId(), null, null));

        webhookService.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        AssinaturaTenant depois = recarregar(trial);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(depois.getMpStatus()).isEqualTo("authorized");
        assertThat(depois.getDataVencimento()).isEqualTo(fimDoTrial);
    }

    @Test
    @DisplayName("Régua: erro do MP ao cancelar uma assinatura não desfaz o cancelamento das outras")
    void erroEmUmItemDaReguaNaoDesfazOsDemais() {
        LocalDateTime suspensaHaMuitoTempo = LocalDateTime.now().minusDays(40);
        AssinaturaTenant comFalhaNoMp = salvarAssinatura(StatusAssinatura.SUSPENSA, PREAPPROVAL_ID, "paused",
                LocalDate.now().minusDays(70));
        comFalhaNoMp.setDataSuspensao(suspensaHaMuitoTempo);
        assinaturaTenantRepository.save(comFalhaNoMp);
        AssinaturaTenant semPreapproval = salvarAssinatura(StatusAssinatura.SUSPENSA, null, null,
                LocalDate.now().minusDays(70));
        semPreapproval.setDataSuspensao(suspensaHaMuitoTempo);
        assinaturaTenantRepository.save(semPreapproval);
        when(mercadoPagoClient.cancelarPreapproval(PREAPPROVAL_ID))
                .thenThrow(new BusinessException("MP fora do ar", HttpStatus.BAD_GATEWAY));

        cobrancaScheduler.aplicarReguaDeCarencia();

        assertThat(recarregar(semPreapproval).getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(recarregar(comFalhaNoMp).getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
    }

    // ========== Pós-revisão da Fase 2 ==========

    private CobrancaTenant cobrancaDaFatura() {
        return cobrancaTenantRepository.findByMpAuthorizedPaymentId(FATURA_ID).orElseThrow();
    }

    @Test
    @DisplayName("H1a - GET da fatura falha 5xx no webhook; o job reprocessa após o backoff e a assinatura fica PAGA e ATIVA")
    void jobReprocessaWebhookQueFalhou() {
        LocalDate vencimento = LocalDate.now().minusDays(8);
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.INADIMPLENTE, PREAPPROVAL_ID, "authorized",
                vencimento);
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID))
                .thenThrow(new BusinessException("Não foi possível comunicar com o Mercado Pago",
                        HttpStatus.BAD_GATEWAY))
                .thenReturn(fatura("approved"));

        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");

        MpWebhookEvento comErro = eventoDaFatura();
        assertThat(comErro.getProcessado()).isFalse();
        assertThat(comErro.getErro()).contains("Mercado Pago");
        assertThat(comErro.getTentativas()).isEqualTo(1);
        assertThat(comErro.getProximaTentativaEm()).isAfter(LocalDateTime.now());

        // Dentro do backoff o job não tenta de novo
        cobrancaScheduler.reprocessarWebhooksComErro();
        verify(mercadoPagoClient, times(1)).buscarAuthorizedPayment(FATURA_ID);

        // Passado o backoff, o job reprocessa
        comErro.setProximaTentativaEm(LocalDateTime.now().minusMinutes(1));
        eventoRepository.save(comErro);
        cobrancaScheduler.reprocessarWebhooksComErro();

        assertThat(cobrancaDaFatura().getStatus()).isEqualTo(StatusCobranca.PAGO);
        AssinaturaTenant depois = recarregar(assinatura);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getDataVencimento()).isAfter(LocalDate.now());
        MpWebhookEvento encerrado = eventoDaFatura();
        assertThat(encerrado.getProcessado()).isTrue();
        assertThat(encerrado.getErro()).isNull();
    }

    @Test
    @DisplayName("L3 - Evento com erro mais antigo que o prazo é marcado como expirado e não é mais tentado")
    void eventoAntigoComErroExpira() {
        eventoRepository.save(MpWebhookEvento.builder()
                .tipo(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT)
                .dataId(FATURA_ID)
                .payload("{}")
                .processado(false)
                .erro("timeout")
                .tentativas(9)
                .recebidoEm(LocalDateTime.now().minusDays(4))
                .build());

        cobrancaScheduler.reprocessarWebhooksComErro();

        assertThat(eventoDaFatura().getExpiradoEm()).isNotNull();
        assertThat(eventoDaFatura().getProcessado()).isFalse();
        // N7 - expirado fica marcado para tratamento manual
        assertThat(eventoDaFatura().getRequerAtencao()).isTrue();
        assertThat(eventoDaFatura().getMotivoAtencao()).startsWith(MpWebhookEvento.MOTIVO_EVENTO_EXPIRADO);
        verify(mercadoPagoClient, never()).buscarAuthorizedPayment(anyString());
    }

    @Test
    @DisplayName("H1b - Fatura aprovada no MP sem webhook: a reconciliação registra PAGO e renova")
    void reconciliacaoRecuperaFaturaAprovadaSemWebhook() {
        LocalDate vencimento = LocalDate.now().minusDays(8);
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.INADIMPLENTE, PREAPPROVAL_ID, "authorized",
                vencimento);
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID)).thenReturn(List.of(fatura("approved")));
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID)).thenReturn(fatura("approved"));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(new PreapprovalResponse(
                PREAPPROVAL_ID, "authorized", null, null, null, null, null));

        cobrancaScheduler.reconciliarComMercadoPago();

        assertThat(cobrancaDaFatura().getStatus()).isEqualTo(StatusCobranca.PAGO);
        AssinaturaTenant depois = recarregar(assinatura);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getDataVencimento()).isAfter(LocalDate.now());

        // Segunda reconciliação: a fatura já está PAGA, nada é reprocessado nem renovado de novo
        LocalDate vencimentoRenovado = depois.getDataVencimento();
        cobrancaScheduler.reconciliarComMercadoPago();
        verify(mercadoPagoClient, times(1)).buscarAuthorizedPayment(FATURA_ID);
        assertThat(recarregar(assinatura).getDataVencimento()).isEqualTo(vencimentoRenovado);
    }

    @Test
    @DisplayName("H1c - Régua não suspende quem tem fatura aprovada no MP: registra o pagamento e reativa")
    void reguaNaoSuspendeQuemPagou() {
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.INADIMPLENTE, PREAPPROVAL_ID, "authorized",
                LocalDate.now().minusDays(20));
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID)).thenReturn(List.of(fatura("approved")));
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID)).thenReturn(fatura("approved"));

        cobrancaScheduler.aplicarReguaDeCarencia();

        assertThat(recarregar(assinatura).getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(cobrancaDaFatura().getStatus()).isEqualTo(StatusCobranca.PAGO);
        verify(mercadoPagoClient, never()).pausarPreapproval(anyString());
    }

    @Test
    @DisplayName("H2 - Item da régua renovado entre a leitura do lote e o processamento é ignorado")
    void itemRenovadoDepoisDaLeituraDoLoteEhIgnorado() {
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.INADIMPLENTE, PREAPPROVAL_ID, "authorized",
                LocalDate.now().minusDays(20));
        // O lote já foi lido; enquanto o item espera, um pagamento (webhook) renova a assinatura e faz commit
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval(PREAPPROVAL_ID)).thenAnswer(inv -> {
            assinaturaTenantService.renovarAposPagamento(assinatura.getId());
            return List.of();
        });

        cobrancaScheduler.aplicarReguaDeCarencia();

        AssinaturaTenant depois = recarregar(assinatura);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getDataSuspensao()).isNull();
        verify(mercadoPagoClient, never()).pausarPreapproval(anyString());
    }

    @Test
    @DisplayName("M2 - Pagamento aprovado em assinatura CANCELADA registra PAGO e não reativa")
    void pagamentoEmCanceladaNaoReativa() {
        LocalDate vencimento = LocalDate.now().minusDays(60);
        AssinaturaTenant cancelada = salvarAssinatura(StatusAssinatura.CANCELADA, PREAPPROVAL_ID, "cancelled",
                vencimento);
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID)).thenReturn(fatura("approved"));

        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");

        assertThat(cobrancaDaFatura().getStatus()).isEqualTo(StatusCobranca.PAGO);
        AssinaturaTenant depois = recarregar(cancelada);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(depois.getDataVencimento()).isEqualTo(vencimento);
        assertThat(eventoDaFatura().getProcessado()).isTrue();
        verify(mercadoPagoClient, never()).reativarPreapproval(anyString());
    }

    @Test
    @DisplayName("D2 - Remover cortesia volta para TRIAL; se o MP falhar na hora, a rotina diária de links gera depois")
    void removerCortesiaVoltaParaTrialERotinaGeraLink() {
        AssinaturaTenant cortesia = salvarAssinatura(StatusAssinatura.ATIVA, null, null, LocalDate.now().minusDays(100));
        cortesia.setCortesia(true);
        cortesia.setMpPayerEmail("pagador@clinica-e2e.com");
        cortesia = assinaturaTenantRepository.save(cortesia);
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), anyString(), anyString(), any()))
                .thenThrow(new BusinessException("MP fora do ar", HttpStatus.BAD_GATEWAY))
                .thenReturn(new PreapprovalResponse("pa-e2e-cortesia", "pending", "https://mp/checkout/cortesia",
                        "pagador@clinica-e2e.com", "ASSINATURA-" + cortesia.getId(), null, null));

        assinaturaTenantService.definirCortesia(cortesia.getId(), false);

        AssinaturaTenant semCortesia = recarregar(cortesia);
        assertThat(semCortesia.isCortesia()).isFalse();
        assertThat(semCortesia.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(semCortesia.getDataVencimento()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(semCortesia.getDataProximaCobranca()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(semCortesia.getMpPreapprovalId()).isNull();

        cobrancaScheduler.gerarLinksDeAtivacaoDeTrial();

        AssinaturaTenant comLink = recarregar(cortesia);
        assertThat(comLink.getMpPreapprovalId()).isEqualTo("pa-e2e-cortesia");
        assertThat(comLink.getMpInitPoint()).isEqualTo("https://mp/checkout/cortesia");
        verify(mercadoPagoClient, times(2)).criarPreapprovalPendente(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("H3 - @Version: escrita com cópia velha falha e não apaga a renovação feita por outra transação")
    void escritaComCopiaVelhaFalha() {
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.INADIMPLENTE, PREAPPROVAL_ID, "authorized",
                LocalDate.now().minusDays(8));
        AssinaturaTenant copiaVelha = recarregar(assinatura);

        assinaturaTenantService.renovarAposPagamento(assinatura.getId());
        LocalDate renovado = recarregar(assinatura).getDataVencimento();

        copiaVelha.setStatus(StatusAssinatura.SUSPENSA);
        assertThatThrownBy(() -> assinaturaTenantRepository.save(copiaVelha))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        AssinaturaTenant depois = recarregar(assinatura);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getDataVencimento()).isEqualTo(renovado);
        assertThat(depois.getVersao()).isEqualTo(1L);
    }

    @Test
    @DisplayName("H3 - Webhook concorrente com outra atualização: o commit conflita, o webhook re-lê e reaplica sem perder nada")
    void webhookReaplicaAposConflitoDeVersao() {
        AssinaturaTenant suspensa = salvarAssinatura(StatusAssinatura.SUSPENSA, PREAPPROVAL_ID, "paused",
                LocalDate.now().minusDays(20));
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID)).thenReturn(fatura("approved"));
        TransactionTemplate outraTransacao = new TransactionTemplate(transactionManager);
        outraTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicBoolean concorrenteJaRodou = new AtomicBoolean(false);
        // Durante a 1ª tentativa do webhook (depois de ele ler a assinatura), outra transação a altera e faz commit
        when(mercadoPagoClient.reativarPreapproval(PREAPPROVAL_ID)).thenAnswer(inv -> {
            if (concorrenteJaRodou.compareAndSet(false, true)) {
                outraTransacao.executeWithoutResult(status -> {
                    AssinaturaTenant a = assinaturaTenantRepository.findById(suspensa.getId()).orElseThrow();
                    a.setMpPayerEmail("financeiro@clinica-e2e.com");
                    assinaturaTenantRepository.save(a);
                });
            }
            return new PreapprovalResponse(PREAPPROVAL_ID, "authorized", null, null, null, null, null);
        });

        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");

        AssinaturaTenant depois = recarregar(suspensa);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getDataVencimento()).isAfter(LocalDate.now());
        assertThat(depois.getMpStatus()).isEqualTo("authorized");
        assertThat(depois.getMpPayerEmail()).isEqualTo("financeiro@clinica-e2e.com");
        assertThat(cobrancaDaFatura().getStatus()).isEqualTo(StatusCobranca.PAGO);
        assertThat(cobrancaTenantRepository.findAll().stream()
                .filter(c -> FATURA_ID.equals(c.getMpAuthorizedPaymentId())).count()).isEqualTo(1);
        assertThat(eventoDaFatura().getProcessado()).isTrue();
        verify(mercadoPagoClient, times(2)).reativarPreapproval(PREAPPROVAL_ID);
    }

    @Test
    @DisplayName("D1 - TRIAL vencido segue a régua da ATIVA: +5 dias INADIMPLENTE, +15 dias SUSPENSA")
    void trialVencidoSegueARegua() {
        AssinaturaTenant vencidoHa6Dias = salvarAssinatura(StatusAssinatura.TRIAL, null, null,
                LocalDate.now().minusDays(6));
        AssinaturaTenant vencidoHa16Dias = salvarAssinatura(StatusAssinatura.TRIAL, null, null,
                LocalDate.now().minusDays(16));
        AssinaturaTenant vencidoHa3Dias = salvarAssinatura(StatusAssinatura.TRIAL, null, null,
                LocalDate.now().minusDays(3));

        cobrancaScheduler.aplicarReguaDeCarencia();

        assertThat(recarregar(vencidoHa3Dias).getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(recarregar(vencidoHa6Dias).getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        AssinaturaTenant suspensa = recarregar(vencidoHa16Dias);
        assertThat(suspensa.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(suspensa.getDataSuspensao()).isNotNull();
    }

    @Test
    @DisplayName("N7 - Pagamento em CANCELADA aparece em GET /assinaturas/pendencias-manuais (só SUPER_ADMIN)")
    void pagamentoEmCanceladaApareceNasPendenciasManuais() throws Exception {
        salvarAssinatura(StatusAssinatura.CANCELADA, PREAPPROVAL_ID, "cancelled", LocalDate.now().minusDays(60));
        when(mercadoPagoClient.buscarAuthorizedPayment(FATURA_ID)).thenReturn(fatura("approved"));

        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");

        MpWebhookEvento evento = eventoDaFatura();
        assertThat(evento.getProcessado()).isTrue();
        assertThat(evento.getRequerAtencao()).isTrue();
        assertThat(evento.getMotivoAtencao()).startsWith(MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA);

        mockMvc.perform(get("/assinaturas/pendencias-manuais").with(user("root").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataId").value(FATURA_ID))
                .andExpect(jsonPath("$[0].motivo").value(org.hamcrest.Matchers.startsWith(
                        MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA)))
                .andExpect(jsonPath("$[0].payload").doesNotExist())
                .andExpect(jsonPath("$[0].motivoCodigo").value(MpWebhookEvento.MOTIVO_PAGAMENTO_EM_CANCELADA))
                .andExpect(jsonPath("$[0].organizacaoId").value(organizacao.getId()))
                .andExpect(jsonPath("$[0].organizacaoNome").value(organizacao.getNome()))
                .andExpect(jsonPath("$[0].resolvida").value(false));
        mockMvc.perform(get("/assinaturas/pendencias-manuais").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        // Resolver: sai das abertas e da contagem do dashboard (cache invalidado), entra nas resolvidas
        mockMvc.perform(get("/plataforma/dashboard").with(user("root").roles("SUPER_ADMIN")))
                .andExpect(jsonPath("$.resumo.pendenciasManuais").value(1));
        mockMvc.perform(patch("/assinaturas/pendencias-manuais/" + evento.getId() + "/resolver")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacao\":\"Estornado\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/assinaturas/pendencias-manuais/" + evento.getId() + "/resolver")
                        .with(user("root").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacao\":\"Estornado no painel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvida").value(true))
                .andExpect(jsonPath("$.observacaoResolucao").value("Estornado no painel"));
        assertThat(eventoRepository.countByRequerAtencaoTrue()).isZero();
        mockMvc.perform(get("/plataforma/dashboard").with(user("root").roles("SUPER_ADMIN")))
                .andExpect(jsonPath("$.resumo.pendenciasManuais").value(0));
        mockMvc.perform(get("/assinaturas/pendencias-manuais").with(user("root").roles("SUPER_ADMIN")))
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/assinaturas/pendencias-manuais?resolvidas=true").with(user("root").roles("SUPER_ADMIN")))
                .andExpect(jsonPath("$[0].eventoId").value(evento.getId()));
        mockMvc.perform(patch("/assinaturas/pendencias-manuais/" + evento.getId() + "/resolver")
                        .with(user("root").roles("SUPER_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacao\":\"de novo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDENCIA_JA_RESOLVIDA"));

        // A reentrega da mesma notificação não reabre
        webhookService.processar(MercadoPagoWebhookService.TOPICO_AUTHORIZED_PAYMENT, FATURA_ID, "{}");
        assertThat(eventoRepository.countByRequerAtencaoTrue()).isZero();
    }

    @Test
    @DisplayName("N4 - Reconciliação reativa no MP o preapproval pausado de assinatura ATIVA em dia")
    void reconciliacaoReativaPausadoDeAtivaEmDia() {
        AssinaturaTenant assinatura = salvarAssinatura(StatusAssinatura.ATIVA, PREAPPROVAL_ID, "paused",
                LocalDate.now().plusDays(25));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(new PreapprovalResponse(
                PREAPPROVAL_ID, "paused", null, null, null, null, null));
        when(mercadoPagoClient.reativarPreapproval(PREAPPROVAL_ID)).thenReturn(new PreapprovalResponse(
                PREAPPROVAL_ID, "authorized", null, null, null, null, null));

        cobrancaScheduler.reconciliarComMercadoPago();

        verify(mercadoPagoClient).reativarPreapproval(PREAPPROVAL_ID);
        AssinaturaTenant depois = recarregar(assinatura);
        assertThat(depois.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(depois.getMpStatus()).isEqualTo("authorized");
    }

    @Test
    @DisplayName("N8 - Remover cortesia com preapproval pausado: o antigo só é cancelado após vincular o novo; se o MP falhar a rotina diária tenta de novo sem duplicar")
    void removerCortesiaComPreapprovalPausadoNaoDeixaOrfaoETemRetry() {
        AssinaturaTenant cortesia = salvarAssinatura(StatusAssinatura.ATIVA, "pa-e2e-antigo", "paused",
                LocalDate.now().minusDays(100));
        cortesia.setCortesia(true);
        cortesia.setMpPayerEmail("pagador@clinica-e2e.com");
        cortesia = assinaturaTenantRepository.save(cortesia);
        Long id = cortesia.getId();
        // 1ª consulta: ainda pausado (será cancelado); depois disso o MP já o devolve cancelado
        when(mercadoPagoClient.buscarPreapproval("pa-e2e-antigo"))
                .thenReturn(new PreapprovalResponse("pa-e2e-antigo", "paused", null, null, null, null, null))
                .thenReturn(new PreapprovalResponse("pa-e2e-antigo", "cancelled", null, null, null, null, null));
        when(mercadoPagoClient.cancelarPreapproval("pa-e2e-antigo"))
                .thenReturn(new PreapprovalResponse("pa-e2e-antigo", "cancelled", null, null, null, null, null));
        when(mercadoPagoClient.criarPreapprovalPendente(anyString(), anyString(), anyString(), any()))
                .thenThrow(new BusinessException("MP fora do ar", HttpStatus.BAD_GATEWAY))
                .thenReturn(new PreapprovalResponse("pa-e2e-novo", "pending", "https://mp/checkout/novo",
                        "pagador@clinica-e2e.com", "ASSINATURA-" + id, null, null));

        assinaturaTenantService.definirCortesia(id, false);

        AssinaturaTenant aposFalha = recarregar(cortesia);
        assertThat(aposFalha.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(aposFalha.getMpPreapprovalId()).isEqualTo("pa-e2e-antigo");
        // C3: o antigo só é cancelado depois que o novo foi vinculado; com a criação falhando, fica intacto
        verify(mercadoPagoClient, never()).cancelarPreapproval("pa-e2e-antigo");

        // Rotina diária pega o TRIAL com preapproval não autorizado e gera o link
        cobrancaScheduler.gerarLinksDeAtivacaoDeTrial();

        AssinaturaTenant comLink = recarregar(cortesia);
        assertThat(comLink.getMpPreapprovalId()).isEqualTo("pa-e2e-novo");
        assertThat(comLink.getMpStatus()).isEqualTo("pending");
        assertThat(comLink.getMpInitPoint()).isEqualTo("https://mp/checkout/novo");

        // Com link pendente válido, nova execução não gera outro preapproval
        cobrancaScheduler.gerarLinksDeAtivacaoDeTrial();

        verify(mercadoPagoClient, times(1)).cancelarPreapproval("pa-e2e-antigo");
        verify(mercadoPagoClient, times(2)).criarPreapprovalPendente(anyString(), anyString(), anyString(), any());
    }
}
