package br.com.saudeConecta.application.assinatura;

import static org.assertj.core.api.Assertions.assertThatCode;
import br.com.saudeConecta.application.assinatura.dto.LinkPagamentoResponse;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.notificacao.DadosEmailAtivacaoAssinatura;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssinaturaMercadoPagoService - Testes unitários")
class AssinaturaMercadoPagoServiceTest {

    private static final Long ORG_ID = 7L;
    private static final Long ASSINATURA_ID = 42L;
    private static final String PREAPPROVAL_ID = "2c93808497f9d3a8";
    private static final String INIT_POINT = "https://www.mercadopago.com.br/subscriptions/checkout?preapproval_id=x";
    private static final String EMAIL_ADMIN = "admin@clinica.com.br";

    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private MercadoPagoWebhookService webhookService;
    @Mock private CobrancaTenantRepository cobrancaTenantRepository;
    @Mock private MpWebhookEventoRepository eventoRepository;

    private MercadoPagoProperties properties;
    private AssinaturaMercadoPagoService service;
    private LinkPagamentoAssinaturaService linkService;
    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setAccessToken("token-teste");
        properties.setBackUrl("https://app.saudeconecta.com.br/assinatura/sucesso");

        service = new AssinaturaMercadoPagoService(assinaturaTenantRepository,
                adminOrganizacaoRepository, mercadoPagoClient, properties, emailNotificacaoService,
                cobrancaTenantRepository, eventoRepository, transactionManager);
        // Entrada real do link: recusa por pagamento recuperado + troca/reaproveitamento deste serviço
        linkService = new LinkPagamentoAssinaturaService(assinaturaTenantRepository, service, webhookService);

        Organizacao org = new Organizacao();
        org.setId(ORG_ID);

        PlanoAssinatura plano = new PlanoAssinatura();
        plano.setId(2L);
        plano.setNome("Profissional");

        assinatura = AssinaturaTenant.builder()
                .id(ASSINATURA_ID)
                .organizacao(org)
                .planoAssinatura(plano)
                .status(StatusAssinatura.TRIAL)
                .valorMensal(BigDecimal.valueOf(279))
                .build();

        AdminOrganizacao admin = new AdminOrganizacao();
        admin.setId(3L);
        admin.setEmail(EMAIL_ADMIN);
        admin.setNome("Fulano");

        when(adminOrganizacaoRepository.findOwnerByOrganizacaoId(ORG_ID)).thenReturn(Optional.of(admin));
        when(assinaturaTenantRepository.findAssinaturaVigenteByOrganizacaoId(ORG_ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(ORG_ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaTenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaTenantRepository.findByIdParaAtualizacao(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
    }

    @Test
    @DisplayName("Deve criar preapproval e persistir o link de pagamento")
    void deveCriarPreapprovalEPersistirLink() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = linkService.gerarLinkPagamento(ORG_ID);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        assertThat(link.payerEmail()).isEqualTo(EMAIL_ADMIN);
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getMpStatus()).isEqualTo("pending");
        verify(assinaturaTenantRepository).save(assinatura);
    }

    @Test
    @DisplayName("Deve reaproveitar preapproval pendente em vez de criar outro")
    void deveReaproveitarPreapprovalPendente() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint(INIT_POINT);

        LinkPagamentoResponse link = linkService.gerarLinkPagamento(ORG_ID);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Não deve gerar link para assinatura de cortesia")
    void naoDeveGerarLinkParaCortesia() {
        assinatura.setCortesia(true);

        assertThatThrownBy(() -> linkService.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cortesia");
    }

    @Test
    @DisplayName("Não deve gerar link para assinatura já ativa no Mercado Pago")
    void naoDeveGerarLinkParaAssinaturaAtiva() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);

        assertThatThrownBy(() -> linkService.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está ativa");
    }

    @Test
    @DisplayName("Deve falhar quando a organização não tem e-mail de administrador")
    void deveFalharSemEmailDeAdministrador() {
        when(adminOrganizacaoRepository.findOwnerByOrganizacaoId(ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail de administrador");
    }

    @Test
    @DisplayName("Cancelamento pelo cliente deve propagar ao Mercado Pago e registrar a origem")
    void cancelamentoPeloClienteDevePropagarAoMercadoPago() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("authorized");
        when(mercadoPagoClient.cancelarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null, EMAIL_ADMIN, null, null, null));

        service.cancelarPeloCliente(ORG_ID);

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.getCanceladoPor()).isEqualTo(OrigemCancelamento.CLIENTE);
        assertThat(assinatura.getMpStatus()).isEqualTo("cancelled");
        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
    }

    @Test
    @DisplayName("Cancelar assinatura sem preapproval não deve chamar o Mercado Pago")
    void cancelarSemPreapprovalNaoChamaMercadoPago() {
        service.cancelar(assinatura, OrigemCancelamento.SUPER_ADMIN);

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
    }

    @Test
    @DisplayName("2.3 - Sincronizar com authorized atualiza o espelho do MP mas não mexe no status local nem no vencimento")
    void sincronizarAuthorizedNaoPromoveStatus() {
        LocalDate vencimento = LocalDate.now().plusDays(3);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        assinatura.setDataVencimento(vencimento);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", INIT_POINT, EMAIL_ADMIN, null, null, null));

        boolean mudou = service.sincronizar(assinatura);

        assertThat(mudou).isTrue();
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(assinatura.getDataVencimento()).isEqualTo(vencimento);
    }

    @Test
    @DisplayName("2.3 - Reconciliação não desfaz INADIMPLENTE nem SUSPENSA quando o MP diz authorized")
    void sincronizarNaoDesfazInadimplenciaNemSuspensao() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("authorized");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", INIT_POINT, EMAIL_ADMIN, null, null, null));

        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        assertThat(service.sincronizar(assinatura)).isFalse();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);

        LocalDateTime suspensaEm = LocalDateTime.now().minusDays(10);
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setDataSuspensao(suspensaEm);
        assertThat(service.sincronizar(assinatura)).isFalse();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(assinatura.getDataSuspensao()).isEqualTo(suspensaEm);
    }

    @Test
    @DisplayName("Sincronizar com paused suspende e registra a data da suspensão")
    void sincronizarPausedSuspende() {
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("authorized");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "paused", null, EMAIL_ADMIN, null, null, null));

        assertThat(service.sincronizar(assinatura)).isTrue();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(assinatura.getDataSuspensao()).isNotNull();
    }

    @Test
    @DisplayName("Status remoto: só pausa e cancelamento refletem localmente; cancelada é terminal")
    void aplicarStatusRemoto() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assertThat(service.aplicarStatusRemoto(assinatura, "authorized")).isFalse();
        assertThat(service.aplicarStatusRemoto(assinatura, "pending")).isFalse();
        assertThat(service.aplicarStatusRemoto(assinatura, null)).isFalse();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);

        assertThat(service.aplicarStatusRemoto(assinatura, "cancelled")).isTrue();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.getCanceladoPor()).isEqualTo(OrigemCancelamento.MERCADO_PAGO);

        assertThat(service.aplicarStatusRemoto(assinatura, "paused")).isFalse();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
    }

    @Test
    @DisplayName("TRIAL: cancelamento no MP (recusa no 1º checkout) não encerra a assinatura")
    void cancelamentoNoMpNaoEncerraTrial() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setMpPreapprovalId("pa-recusado");
        assinatura.setMpStatus("cancelled");
        assinatura.setMpInitPoint("https://mp/checkout-morto");

        assertThat(service.aplicarStatusRemoto(assinatura, "cancelled")).isFalse();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(assinatura.getDataCancelamento()).isNull();
        assertThat(assinatura.getCanceladoPor()).isNull();
        // Desvinculado: a reconciliação não o acha mais e o próximo Pagar gera link novo
        assertThat(assinatura.getMpPreapprovalId()).isNull();
        assertThat(assinatura.getMpStatus()).isNull();
        assertThat(assinatura.getMpInitPoint()).isNull();
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("TRIAL: o cancelamento chega antes da recusa e a cobrança RECUSADO é gravada antes de desvincular")
    void cancelamentoNoTrialRegistraRecusaAntesDeDesvincular() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setMpPreapprovalId("pa-recusado");
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-recusado")).thenReturn(List.of(
                new AuthorizedPaymentResponse("fat-rec", "pa-recusado", "processed", BigDecimal.valueOf(549),
                        new AuthorizedPaymentResponse.Payment(7L, "rejected", "cc_rejected_other_reason"))));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("fat-rec")).thenReturn(Optional.empty());

        service.aplicarStatusRemoto(assinatura, "cancelled");

        ArgumentCaptor<CobrancaTenant> captor = ArgumentCaptor.forClass(CobrancaTenant.class);
        verify(cobrancaTenantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCobranca.RECUSADO);
        assertThat(captor.getValue().getMotivoRecusa()).isEqualTo("cc_rejected_other_reason");
        assertThat(captor.getValue().getMpAuthorizedPaymentId()).isEqualTo("fat-rec");
        assertThat(captor.getValue().getValorTotal()).isEqualByComparingTo("549");
        assertThat(assinatura.getMpPreapprovalId()).isNull();
    }

    @Test
    @DisplayName("TRIAL: recusa já registrada pelo webhook não é gravada de novo")
    void cancelamentoNoTrialNaoDuplicaRecusaRegistrada() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setMpPreapprovalId("pa-recusado");
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-recusado")).thenReturn(List.of(
                new AuthorizedPaymentResponse("fat-rec", "pa-recusado", "processed", BigDecimal.valueOf(549),
                        new AuthorizedPaymentResponse.Payment(7L, "rejected", "cc_rejected_other_reason"))));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("fat-rec"))
                .thenReturn(Optional.of(new CobrancaTenant()));

        service.aplicarStatusRemoto(assinatura, "cancelled");

        verify(cobrancaTenantRepository, never()).save(any());
        assertThat(assinatura.getMpPreapprovalId()).isNull();
    }

    @Test
    @DisplayName("TRIAL: preapproval cancelado com fatura aprovada sem registro mantém o vínculo para a recuperação")
    void cancelamentoNoTrialComFaturaPagaSemRegistroMantemVinculo() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setMpPreapprovalId("pa-pago");
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-pago")).thenReturn(List.of(
                new AuthorizedPaymentResponse("fat-1", "pa-pago", "processed", BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1L, "approved", "accredited"))));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("fat-1")).thenReturn(Optional.empty());

        assertThat(service.aplicarStatusRemoto(assinatura, "cancelled")).isFalse();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo("pa-pago");
    }

    @Test
    @DisplayName("Recusa no trial, trial vence e a régua leva a INADIMPLENTE: a reconciliação não pula para CANCELADA")
    void trialComRecusaVencidoSegueNaReguaSemCancelar() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setMpPreapprovalId("pa-recusado");
        assinatura.setMpStatus("pending");
        when(mercadoPagoClient.buscarPreapproval("pa-recusado")).thenReturn(
                new PreapprovalResponse("pa-recusado", "cancelled", null, EMAIL_ADMIN, null, null, null));

        service.sincronizar(assinatura); // reconciliação dentro do trial
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);

        assinatura.setStatus(StatusAssinatura.INADIMPLENTE); // a régua no vencimento do trial
        service.sincronizar(assinatura); // reconciliação seguinte

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(assinatura.getCanceladoPor()).isNull();
        verify(mercadoPagoClient, times(1)).buscarPreapproval("pa-recusado");
    }

    @Test
    @DisplayName("INADIMPLENTE: cancelamento no MP continua encerrando a assinatura")
    void cancelamentoNoMpEncerraInadimplente() {
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);

        assertThat(service.aplicarStatusRemoto(assinatura, "cancelled")).isTrue();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
    }

    @Test
    @DisplayName("Gerar link e notificar: cria o preapproval e envia o e-mail ao administrador")
    void gerarLinkENotificar() {
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        when(assinaturaTenantRepository.findByMpPreapprovalId(PREAPPROVAL_ID)).thenReturn(Optional.of(assinatura));

        LinkPagamentoResponse link = linkService.gerarLinkENotificar(ASSINATURA_ID);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        // M1: o e-mail só sai depois do commit da transação que vinculou o link
        var ordem = inOrder(transactionManager, emailNotificacaoService);
        ordem.verify(transactionManager).commit(any());
        var dados = ArgumentCaptor.forClass(DadosEmailAtivacaoAssinatura.class);
        ordem.verify(emailNotificacaoService).enviarLinkAtivacaoAssinatura(dados.capture());
        assertThat(dados.getValue().email()).isEqualTo(EMAIL_ADMIN);
        assertThat(dados.getValue().nome()).isEqualTo("Fulano");
        assertThat(dados.getValue().plano()).isEqualTo("Profissional");
        assertThat(dados.getValue().initPoint()).isEqualTo(INIT_POINT);
        assertThat(dados.getValue().payerEmail()).isEqualTo(EMAIL_ADMIN);
        assertThat(dados.getValue().organizacaoId()).isEqualTo(ORG_ID);
        assertThat(dados.getValue().adminOrgId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("M1 - paused não rebaixa ATIVA com vencimento no futuro (pagamento já confirmado)")
    void pausedNaoRebaixaAtivaEmDia() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().plusDays(20));

        assertThat(service.aplicarStatusRemoto(assinatura, "paused")).isFalse();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getDataSuspensao()).isNull();
    }

    @Test
    @DisplayName("M1 - paused ainda suspende ATIVA já vencida")
    void pausedSuspendeAtivaVencida() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().minusDays(1));

        assertThat(service.aplicarStatusRemoto(assinatura, "paused")).isTrue();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
    }

    @Test
    @DisplayName("M1 - Reativação de preapproval pausado: falha do MP não propaga (só log)")
    void reativarFalhaNaoPropaga() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.reativarPreapproval(PREAPPROVAL_ID))
                .thenThrow(new BusinessException("MP fora", org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatCode(() -> service.reativarSePausado(assinatura)).doesNotThrowAnyException();
        assertThat(assinatura.getMpStatus()).isEqualTo("paused");
    }

    @Test
    @DisplayName("M1 - Preapproval que não está pausado não é reativado")
    void naoReativaQuemNaoEstaPausado() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("authorized");

        service.reativarSePausado(assinatura);

        verify(mercadoPagoClient, never()).reativarPreapproval(any());
    }

    @Test
    @DisplayName("D2 - Próxima cobrança: next_payment_date quando o preapproval está authorized no MP")
    void proximaCobrancaAutorizada() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(new PreapprovalResponse(
                PREAPPROVAL_ID, "authorized", null, null, null, null, "2026-11-20T10:00:00.000-04:00"));

        assertThat(service.buscarProximaCobrancaAutorizada(assinatura)).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    @DisplayName("D2 - Próxima cobrança: null quando o preapproval não está authorized ou não existe")
    void proximaCobrancaNaoAutorizada() {
        assertThat(service.buscarProximaCobrancaAutorizada(assinatura)).isNull();

        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(new PreapprovalResponse(
                PREAPPROVAL_ID, "paused", null, null, null, null, "2026-11-20T10:00:00.000-04:00"));
        assertThat(service.buscarProximaCobrancaAutorizada(assinatura)).isNull();
    }

    @Test
    @DisplayName("N4 - Reconciliação: ATIVA em dia com preapproval paused no MP é reativada no MP")
    void sincronizarReativaPausadoDeAtivaEmDia() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().plusDays(20));
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "paused", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.reativarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", null, EMAIL_ADMIN, null, null, null));

        assertThat(service.sincronizar(assinatura)).isTrue();

        verify(mercadoPagoClient).reativarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
    }

    @Test
    @DisplayName("N4 - Reconciliação: paused em assinatura vencida/inadimplente não reativa (a régua decide)")
    void sincronizarNaoReativaPausadoDeInadimplente() {
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        assinatura.setDataVencimento(LocalDate.now().minusDays(10));
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "paused", null, EMAIL_ADMIN, null, null, null));

        service.sincronizar(assinatura);

        verify(mercadoPagoClient, never()).reativarPreapproval(any());
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
    }

    @Test
    @DisplayName("C3 - Link novo: cria e vincula o novo primeiro; o antigo só é cancelado depois, e a coluna é limpa")
    void gerarLinkCriaNovoAntesDeCancelarAntigo() {
        assinatura.setMpPreapprovalId("pa-antigo");
        assinatura.setMpStatus("paused");
        assinatura.setDiaCobranca(5);
        when(mercadoPagoClient.buscarPreapproval("pa-antigo")).thenReturn(
                new PreapprovalResponse("pa-antigo", "paused", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.cancelarPreapproval("pa-antigo")).thenReturn(
                new PreapprovalResponse("pa-antigo", "cancelled", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        var ordem = inOrder(mercadoPagoClient, assinaturaTenantRepository, transactionManager);
        ordem.verify(assinaturaTenantRepository).findByIdParaAtualizacao(ASSINATURA_ID);
        ordem.verify(mercadoPagoClient).criarPreapprovalPendente(any(), any(), any(), any());
        ordem.verify(assinaturaTenantRepository).save(assinatura);
        ordem.verify(transactionManager).commit(any());
        ordem.verify(mercadoPagoClient).cancelarPreapproval("pa-antigo");
        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
        // A1: o próximo pagamento aprovado do preapproval novo define a âncora
        assertThat(assinatura.getDiaCobranca()).isNull();
    }

    @Test
    @DisplayName("Recusa no checkout: pendente cancelado pelo MP não é reaproveitado, o Pagar gera link novo")
    void pendenteCanceladoNoMpGeraLinkNovo() {
        assinatura.setMpPreapprovalId("pa-recusado");
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint("https://mp/checkout-morto");
        when(mercadoPagoClient.buscarPreapproval("pa-recusado")).thenReturn(
                new PreapprovalResponse("pa-recusado", "cancelled", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getMpStatus()).isEqualTo("pending");
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
        // Já cancelado no MP: não cancela de novo
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
    }

    @Test
    @DisplayName("Pendente cancelado no MP com fatura paga sem registro: 409 PAGAMENTO_JA_RECEBIDO, sem link novo")
    void pendenteCanceladoComFaturaPagaNaoGeraLinkNovo() {
        assinatura.setMpPreapprovalId("pa-pago");
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint("https://mp/checkout");
        when(mercadoPagoClient.buscarPreapproval("pa-pago")).thenReturn(
                new PreapprovalResponse("pa-pago", "cancelled", null, EMAIL_ADMIN, null, null, null));
        when(webhookService.recuperarFaturasAprovadas("pa-pago")).thenReturn(1);

        assertThatThrownBy(() -> linkService.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("PAGAMENTO_JA_RECEBIDO"));
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Pendente com resposta do MP de outro id: não confia nela e reaproveita o link")
    void pendenteComIdDivergenteReaproveita() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint(INIT_POINT);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse("outro-id", "cancelled", null, EMAIL_ADMIN, null, null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Pagar depois da recusa: a recusa do link cancelado vai para o histórico antes da troca")
    void pendenteCanceladoRegistraRecusaAntesDaTroca() {
        assinatura.setMpPreapprovalId("pa-recusado");
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint("https://mp/checkout-morto");
        when(mercadoPagoClient.buscarPreapproval("pa-recusado")).thenReturn(
                new PreapprovalResponse("pa-recusado", "cancelled", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-recusado")).thenReturn(List.of(
                new AuthorizedPaymentResponse("fat-rec", "pa-recusado", "recycling", BigDecimal.valueOf(549),
                        new AuthorizedPaymentResponse.Payment(7L, "rejected", "cc_rejected_other_reason"))));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("fat-rec")).thenReturn(Optional.empty());
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        ArgumentCaptor<CobrancaTenant> captor = ArgumentCaptor.forClass(CobrancaTenant.class);
        verify(cobrancaTenantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCobranca.RECUSADO);
        assertThat(captor.getValue().getMpAuthorizedPaymentId()).isEqualTo("fat-rec");
        assertThat(captor.getValue().getMotivoRecusa()).isEqualTo("cc_rejected_other_reason");
        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.TRIAL);
    }

    @Test
    @DisplayName("Pendente ainda pendente no MP: reaproveita o link sem criar outro")
    void pendenteAindaPendenteNoMpReaproveita() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint(INIT_POINT);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT, EMAIL_ADMIN, null, null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
        verify(assinaturaTenantRepository, never()).findByIdParaAtualizacao(any());
    }

    @Test
    @DisplayName("Pendente com MP fora do ar na consulta: reaproveita o link como antes")
    void pendenteComMpIndisponivelReaproveita() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint(INIT_POINT);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenThrow(
                new BusinessException("MP fora", org.springframework.http.HttpStatus.BAD_GATEWAY));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Pendente autorizado no MP (webhook atrasado): reaproveita, nunca troca nem cancela o que o cliente pagou")
    void pendenteAutorizadoNoMpNaoTroca() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        assinatura.setMpInitPoint(INIT_POINT);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", INIT_POINT, EMAIL_ADMIN, null, null, null));

        service.gerarLinkPagamento(assinatura);

        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
    }

    @Test
    @DisplayName("C3 - Preapproval antigo já cancelado no MP: não cancela de novo (retry idempotente)")
    void gerarLinkNaoCancelaDeNovoQuemJaEstaCancelado() {
        assinatura.setMpPreapprovalId("pa-antigo");
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.buscarPreapproval("pa-antigo")).thenReturn(
                new PreapprovalResponse("pa-antigo", "cancelled", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        service.gerarLinkPagamento(assinatura);

        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("C3 - Falha no POST do novo: o antigo segue vinculado e intacto no MP")
    void falhaAoCriarNovoNaoMexeNoAntigo() {
        assinatura.setMpPreapprovalId("pa-antigo");
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenThrow(new BusinessException("MP fora", org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.gerarLinkPagamento(assinatura)).isInstanceOf(BusinessException.class);

        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        verify(transactionManager).rollback(any());
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo("pa-antigo");
        assertThat(assinatura.getMpStatus()).isEqualTo("paused");
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("C3 - Falha ao cancelar o antigo depois do commit: link novo vale e o antigo fica pendente para a reconciliação")
    void falhaAoCancelarAntigoDeixaPendente() {
        assinatura.setMpPreapprovalId("pa-antigo");
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.buscarPreapproval("pa-antigo")).thenReturn(
                new PreapprovalResponse("pa-antigo", "paused", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.cancelarPreapproval("pa-antigo"))
                .thenThrow(new BusinessException("MP fora", org.springframework.http.HttpStatus.BAD_GATEWAY));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo("pa-antigo");

        // Reconciliação: o MP voltou, o antigo é cancelado e a coluna é limpa
        reset(mercadoPagoClient);
        when(mercadoPagoClient.buscarPreapproval("pa-antigo")).thenReturn(
                new PreapprovalResponse("pa-antigo", "paused", null, EMAIL_ADMIN, null, null, null));
        assertThat(service.cancelarPreapprovalSubstituido(ASSINATURA_ID)).isTrue();
        verify(mercadoPagoClient).cancelarPreapproval("pa-antigo");
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("C3 - Substituído anterior ainda pendente é cancelado antes de trocar de novo (uma coluna só)")
    void substituidoAnteriorPendenteEhCanceladoAntes() {
        assinatura.setMpPreapprovalId("pa-b");
        assinatura.setMpStatus("paused");
        assinatura.setMpPreapprovalSubstituidoId("pa-a");
        when(mercadoPagoClient.buscarPreapproval(any())).thenAnswer(inv -> new PreapprovalResponse(
                inv.getArgument(0), "paused", null, EMAIL_ADMIN, null, null, null));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse("pa-c", "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        service.gerarLinkPagamento(assinatura);

        var ordem = inOrder(mercadoPagoClient);
        ordem.verify(mercadoPagoClient).cancelarPreapproval("pa-a");
        ordem.verify(mercadoPagoClient).criarPreapprovalPendente(any(), any(), any(), any());
        ordem.verify(mercadoPagoClient).cancelarPreapproval("pa-b");
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo("pa-c");
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("M1 - Erro depois de criar o preapproval e antes do commit: cancela o criado (compensação) e não envia e-mail")
    void erroAntesDoCommitCompensaPreapprovalCriado() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));
        when(assinaturaTenantRepository.save(any())).thenThrow(new IllegalStateException("banco caiu"));
        // 1ª leitura: gerarLinkENotificar; 2ª: releitura da compensação, que vê o banco sem o vínculo
        when(assinaturaTenantRepository.findById(ASSINATURA_ID))
                .thenReturn(Optional.of(assinatura))
                .thenReturn(Optional.of(AssinaturaTenant.builder().id(ASSINATURA_ID).build()));

        assertThatThrownBy(() -> linkService.gerarLinkENotificar(ASSINATURA_ID)).isInstanceOf(IllegalStateException.class);

        verify(transactionManager).rollback(any());
        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
        verifyNoInteractions(emailNotificacaoService);
    }

    @Test
    @DisplayName("M1 - MP cria o preapproval sem init_point: 502 e o preapproval criado é cancelado")
    void preapprovalSemInitPointEhCompensado() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", null,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        assertThatThrownBy(() -> service.gerarLinkPagamento(assinatura))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(org.springframework.http.HttpStatus.BAD_GATEWAY);

        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalId()).isNull();
    }

    @Test
    @DisplayName("M1 - Falha na compensação não esconde o erro original")
    void falhaNaCompensacaoNaoEscondeErroOriginal() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));
        when(assinaturaTenantRepository.save(any())).thenThrow(new IllegalStateException("banco caiu"));
        when(assinaturaTenantRepository.findById(ASSINATURA_ID))
                .thenReturn(Optional.of(AssinaturaTenant.builder().id(ASSINATURA_ID).build()));
        when(mercadoPagoClient.cancelarPreapproval(PREAPPROVAL_ID))
                .thenThrow(new BusinessException("MP fora", org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.gerarLinkPagamento(assinatura))
                .isInstanceOf(IllegalStateException.class).hasMessage("banco caiu");
        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
    }

    @Test
    @DisplayName("R5 - Commit \"falhou\" mas o preapproval novo ficou gravado: a compensação relê e não cancela")
    void compensacaoNaoCancelaPreapprovalJaGravado() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));
        // A confirmação do COMMIT se perdeu, mas o banco gravou: a releitura enxerga o vínculo
        doThrow(new org.springframework.transaction.TransactionSystemException("conexão caiu no commit"))
                .when(transactionManager).commit(any());

        assertThatThrownBy(() -> service.gerarLinkPagamento(assinatura))
                .isInstanceOf(org.springframework.transaction.TransactionSystemException.class);

        verify(assinaturaTenantRepository).findById(ASSINATURA_ID);
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
    }

    @Test
    @DisplayName("R5 - Releitura da compensação falhou: na dúvida não cancela")
    void compensacaoSemReleituraNaoCancela() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));
        when(assinaturaTenantRepository.save(any())).thenThrow(new IllegalStateException("banco caiu"));
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenThrow(new IllegalStateException("banco caiu"));

        assertThatThrownBy(() -> service.gerarLinkPagamento(assinatura))
                .isInstanceOf(IllegalStateException.class);

        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
    }

    @Test
    @DisplayName("M2 - Fatura aprovada do preapproval atual recuperada: 409 PAGAMENTO_JA_RECEBIDO e nenhum link novo")
    void pagamentoRecuperadoRecusaLinkNovo() {
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setMpPreapprovalId("pa-antigo");
        assinatura.setMpStatus("paused");
        when(webhookService.recuperarFaturasAprovadas("pa-antigo")).thenReturn(1);

        assertThatThrownBy(() -> linkService.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
                    assertThat(be.getErrorCode()).isEqualTo("PAGAMENTO_JA_RECEBIDO");
                });

        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo("pa-antigo");
    }

    @Test
    @DisplayName("M2 - Sem fatura a recuperar o link novo é gerado; sem preapproval não consulta faturas")
    void semPagamentoRecuperadoSegueComLinkNovo() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        linkService.gerarLinkPagamento(ORG_ID);
        verify(webhookService, never()).recuperarFaturasAprovadas(any());

        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setMpStatus("paused");
        when(webhookService.recuperarFaturasAprovadas(PREAPPROVAL_ID)).thenReturn(0);
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse("pa-novo", "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        linkService.gerarLinkPagamento(ORG_ID);

        verify(webhookService).recuperarFaturasAprovadas(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo("pa-novo");
    }

    private void comPreapprovalNoMp(String statusMp) {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus(statusMp);
        assinatura.setMpInitPoint(INIT_POINT);
        assinatura.setValorMensal(BigDecimal.valueOf(399));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, statusMp, INIT_POINT, EMAIL_ADMIN, null, null, null));
    }

    @Test
    @DisplayName("C2(b) - Preapproval authorized: atualiza o valor no MP e mantém o vínculo")
    void valorNovoComPreapprovalAutorizado() {
        comPreapprovalNoMp("authorized");

        service.propagarNovoValor(assinatura, BigDecimal.valueOf(279));

        verify(mercadoPagoClient).atualizarValor(PREAPPROVAL_ID, BigDecimal.valueOf(399));
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(assinatura.getMpInitPoint()).isEqualTo(INIT_POINT);
    }

    @Test
    @DisplayName("C2(b) - Preapproval paused: também atualiza o valor no MP")
    void valorNovoComPreapprovalPausado() {
        comPreapprovalNoMp("paused");

        service.propagarNovoValor(assinatura, BigDecimal.valueOf(279));

        verify(mercadoPagoClient).atualizarValor(PREAPPROVAL_ID, BigDecimal.valueOf(399));
    }

    @Test
    @DisplayName("C2(b) - MP falha ao atualizar: 502 MP_VALOR_NAO_ATUALIZADO (o chamador desfaz a troca)")
    void falhaDoMpAoAtualizarValor() {
        comPreapprovalNoMp("authorized");
        when(mercadoPagoClient.atualizarValor(any(), any()))
                .thenThrow(new BusinessException("MP fora", org.springframework.http.HttpStatus.BAD_GATEWAY));

        BigDecimal novoValor = BigDecimal.valueOf(279);
        assertThatThrownBy(() -> service.propagarNovoValor(assinatura, novoValor))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_GATEWAY);
                    assertThat(be.getErrorCode()).isEqualTo("MP_VALOR_NAO_ATUALIZADO");
                });
    }

    private static PreapprovalResponse comValor(String id, String status, String valor) {
        return new PreapprovalResponse(id, status, null, EMAIL_ADMIN, null,
                new PreapprovalResponse.AutoRecurring(1, "months", new BigDecimal(valor), "BRL"), null);
    }

    @Test
    @DisplayName("R1 - Timeout no PUT do valor, mas o MP aplicou: a releitura confirma e a troca segue")
    void timeoutNoPutComValorJaAplicadoSegue() {
        comPreapprovalNoMp("authorized");
        when(mercadoPagoClient.atualizarValor(any(), any()))
                .thenThrow(new BusinessException("timeout", org.springframework.http.HttpStatus.BAD_GATEWAY));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(comValor(PREAPPROVAL_ID, "authorized", "279.00"))
                .thenReturn(comValor(PREAPPROVAL_ID, "authorized", "399.00"));

        assertThatCode(() -> service.propagarNovoValor(assinatura, BigDecimal.valueOf(279)))
                .doesNotThrowAnyException();

        verify(mercadoPagoClient, times(2)).buscarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
    }

    @Test
    @DisplayName("R1 - Timeout no PUT do valor e o MP não aplicou: 502 MP_VALOR_NAO_ATUALIZADO")
    void timeoutNoPutSemAplicarDevolve502() {
        comPreapprovalNoMp("authorized");
        when(mercadoPagoClient.atualizarValor(any(), any()))
                .thenThrow(new BusinessException("timeout", org.springframework.http.HttpStatus.BAD_GATEWAY));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(comValor(PREAPPROVAL_ID, "authorized", "279"));

        BigDecimal novoValor = BigDecimal.valueOf(279);
        assertThatThrownBy(() -> service.propagarNovoValor(assinatura, novoValor))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("MP_VALOR_NAO_ATUALIZADO"));
    }

    @Test
    @DisplayName("R1 - Timeout no PUT e a releitura também falha: 502 (valor incerto, a reconciliação acerta)")
    void timeoutNoPutEReleituraFalhaDevolve502() {
        comPreapprovalNoMp("authorized");
        when(mercadoPagoClient.atualizarValor(any(), any()))
                .thenThrow(new BusinessException("timeout", org.springframework.http.HttpStatus.BAD_GATEWAY));
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(comValor(PREAPPROVAL_ID, "authorized", "279"))
                .thenThrow(new BusinessException("timeout", org.springframework.http.HttpStatus.BAD_GATEWAY));

        BigDecimal novoValor = BigDecimal.valueOf(279);
        assertThatThrownBy(() -> service.propagarNovoValor(assinatura, novoValor))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("MP_VALOR_NAO_ATUALIZADO"));
    }

    @Test
    @DisplayName("R1 - Reconciliação: valor do preapproval diferente do banco é corrigido no MP")
    void reconciliacaoCorrigeValorDivergente() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().plusDays(10));
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("authorized");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(comValor(PREAPPROVAL_ID, "authorized", "399"));

        assertThat(service.sincronizar(assinatura)).isTrue();
        verify(mercadoPagoClient).atualizarValor(PREAPPROVAL_ID, BigDecimal.valueOf(279));

        // Valor igual (escala diferente) ou preapproval pendente: nada a corrigir
        reset(mercadoPagoClient);
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(comValor(PREAPPROVAL_ID, "authorized", "279.00"));
        assertThat(service.sincronizar(assinatura)).isFalse();
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(comValor(PREAPPROVAL_ID, "pending", "399"));
        assinatura.setMpStatus("pending");
        assertThat(service.sincronizar(assinatura)).isFalse();
        verify(mercadoPagoClient, never()).atualizarValor(any(), any());
    }

    @Test
    @DisplayName("C2(c) - Preapproval pending: abandona o link (desvincula e cancela depois); o próximo Pagar cria outro com o valor novo")
    void valorNovoComLinkPendenteAbandonaOLink() {
        comPreapprovalNoMp("pending");
        assinatura.setStatus(StatusAssinatura.TRIAL);

        service.propagarNovoValor(assinatura, BigDecimal.valueOf(279));

        verify(mercadoPagoClient, never()).atualizarValor(any(), any());
        assertThat(assinatura.getMpPreapprovalId()).isNull();
        assertThat(assinatura.getMpInitPoint()).isNull();
        // Sem transação no teste, o "após o commit" roda na hora: o pendente é cancelado e a coluna limpa
        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();

        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), eq(BigDecimal.valueOf(399))))
                .thenReturn(new PreapprovalResponse("pa-novo", "pending", "https://mp/checkout/novo",
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));
        LinkPagamentoResponse link = service.gerarLinkPagamento(assinatura);
        assertThat(link.initPoint()).isEqualTo("https://mp/checkout/novo");
        assertThat(link.valorMensal()).isEqualByComparingTo("399");
    }

    @Test
    @DisplayName("C2(d) - Sem preapproval, cancelado ou valor igual: nada é chamado no MP")
    void valorNovoSemPreapprovalSoBanco() {
        service.propagarNovoValor(assinatura, BigDecimal.valueOf(100));
        verifyNoInteractions(mercadoPagoClient);

        comPreapprovalNoMp("authorized");
        service.propagarNovoValor(assinatura, BigDecimal.valueOf(399));
        verify(mercadoPagoClient, never()).buscarPreapproval(any());

        comPreapprovalNoMp("cancelled");
        service.propagarNovoValor(assinatura, BigDecimal.valueOf(279));
        verify(mercadoPagoClient, never()).atualizarValor(any(), any());
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
    }

    @Test
    @DisplayName("A2 - Cortesia: desvincula o preapproval, cancela no MP depois do commit e a reconciliação não toca nela")
    void cortesiaDesvinculaECancela() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCortesia(true);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "paused", null, EMAIL_ADMIN, null, null, null));

        service.desvincularECancelarAposCommit(assinatura, "CORTESIA");

        assertThat(assinatura.temPreapproval()).isFalse();
        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();

        // Mesmo com um preapproval pausado vinculado (dado antigo), a reconciliação não reativa cortesia
        assinatura.setMpPreapprovalId("pa-legado");
        assinatura.setMpStatus("paused");
        assinatura.setDataVencimento(LocalDate.now().plusDays(10));
        assertThat(service.sincronizar(assinatura)).isFalse();
        verify(mercadoPagoClient, never()).reativarPreapproval(any());
    }

    private PreapprovalResponse noMp(String id, String status) {
        return new PreapprovalResponse(id, status, null, EMAIL_ADMIN, null, null, null);
    }

    @Test
    @DisplayName("R2 - Link pendente abandonado e o cliente termina o checkout: authorized vira pendência, não é cancelado")
    void linkAbandonadoAutorizadoViraPendencia() {
        comPreapprovalNoMp("pending");
        assinatura.setStatus(StatusAssinatura.TRIAL);
        // Entre a troca de plano e o cancelamento, o cliente cadastrou o cartão no checkout do A
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID))
                .thenReturn(noMp(PREAPPROVAL_ID, "pending"))
                .thenReturn(noMp(PREAPPROVAL_ID, "authorized"));
        when(eventoRepository.findByTipoAndDataId(any(), any())).thenReturn(Optional.empty());

        service.propagarNovoValor(assinatura, BigDecimal.valueOf(279));

        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        assertThat(assinatura.getMpPreapprovalId()).isNull();
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo(PREAPPROVAL_ID);
        var pendencia = org.mockito.ArgumentCaptor.forClass(MpWebhookEvento.class);
        verify(eventoRepository).save(pendencia.capture());
        assertThat(pendencia.getValue().getTipo()).isEqualTo(MpWebhookEvento.TIPO_PENDENCIA_PREAPPROVAL_SUBSTITUIDO);
        assertThat(pendencia.getValue().getDataId()).isEqualTo(PREAPPROVAL_ID);
        assertThat(pendencia.getValue().getRequerAtencao()).isTrue();
        assertThat(pendencia.getValue().getProcessado()).isTrue();
        assertThat(pendencia.getValue().getMotivoAtencao()).startsWith("PREAPPROVAL_SUBSTITUIDO_AUTORIZADO");
    }

    @Test
    @DisplayName("R2 - Substituído com fatura aprovada sem registro: pendência; a nova tentativa não duplica")
    void substituidoComFaturaAprovadaViraPendencia() {
        assinatura.setMpPreapprovalSubstituidoId("pa-antigo");
        when(mercadoPagoClient.buscarPreapproval("pa-antigo")).thenReturn(noMp("pa-antigo", "paused"));
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-antigo")).thenReturn(List.of(
                new AuthorizedPaymentResponse("fat-1", "pa-antigo", "processed", BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1L, "approved", "accredited"))));
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("fat-1")).thenReturn(Optional.empty());
        when(eventoRepository.findByTipoAndDataId(MpWebhookEvento.TIPO_PENDENCIA_PREAPPROVAL_SUBSTITUIDO, "pa-antigo"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new MpWebhookEvento()));

        assertThat(service.cancelarPreapprovalSubstituido(ASSINATURA_ID)).isFalse();
        assertThat(service.cancelarPreapprovalSubstituido(ASSINATURA_ID)).isFalse();

        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        verify(eventoRepository, times(1)).save(any());
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo("pa-antigo");
    }

    @Test
    @DisplayName("R2 - Faturas antigas já registradas como PAGO e status não authorized: cancela normalmente")
    void substituidoComFaturasJaRegistradasEhCancelado() {
        assinatura.setMpPreapprovalSubstituidoId("pa-antigo");
        when(mercadoPagoClient.buscarPreapproval("pa-antigo")).thenReturn(noMp("pa-antigo", "paused"));
        when(mercadoPagoClient.buscarAuthorizedPaymentsDoPreapproval("pa-antigo")).thenReturn(List.of(
                new AuthorizedPaymentResponse("fat-1", "pa-antigo", "processed", BigDecimal.valueOf(279),
                        new AuthorizedPaymentResponse.Payment(1L, "approved", "accredited"))));
        br.com.saudeConecta.domain.planos.CobrancaTenant paga = new br.com.saudeConecta.domain.planos.CobrancaTenant();
        paga.setStatus(br.com.saudeConecta.domain.planos.StatusCobranca.PAGO);
        when(cobrancaTenantRepository.findByMpAuthorizedPaymentId("fat-1")).thenReturn(Optional.of(paga));

        assertThat(service.cancelarPreapprovalSubstituido(ASSINATURA_ID)).isTrue();

        verify(mercadoPagoClient).cancelarPreapproval("pa-antigo");
        verify(eventoRepository, never()).save(any());
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("R2 - Cortesia de quem já tinha cartão authorized: abandono deliberado, cancela normalmente")
    void cortesiaComCartaoConhecidoCancela() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCortesia(true);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("authorized");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(noMp(PREAPPROVAL_ID, "authorized"));

        service.desvincularECancelarAposCommit(assinatura, "CORTESIA");

        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
        verify(eventoRepository, never()).save(any());
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("R2 - Sem preapproval vinculado e com o abandonado autorizado: Pagar de novo recebe 409 (evita pagar duas vezes)")
    void pagarDeNovoComAbandonadoAutorizadoRecebe409() {
        assinatura.setMpPreapprovalSubstituidoId("pa-abandonado");
        when(mercadoPagoClient.buscarPreapproval("pa-abandonado")).thenReturn(noMp("pa-abandonado", "authorized"));
        when(eventoRepository.findByTipoAndDataId(any(), any())).thenReturn(Optional.of(new MpWebhookEvento()));

        assertThatThrownBy(() -> linkService.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo("PREAPPROVAL_SUBSTITUIDO_EM_ANALISE"));

        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo("pa-abandonado");
    }

    @Test
    @DisplayName("R2 - Sem preapproval vinculado e com o abandonado ainda pendente: cancela o antigo e gera o link")
    void pagarDeNovoComAbandonadoPendenteCancelaEGera() {
        assinatura.setMpPreapprovalSubstituidoId("pa-abandonado");
        when(mercadoPagoClient.buscarPreapproval("pa-abandonado")).thenReturn(noMp("pa-abandonado", "pending"));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = linkService.gerarLinkPagamento(ORG_ID);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(mercadoPagoClient).cancelarPreapproval("pa-abandonado");
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
    }

    @Test
    @DisplayName("R2 - Substituído anterior em pendência ocupa a coluna: troca nova é recusada com 409")
    void substituidoAnteriorEmPendenciaRecusaTroca() {
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setMpPreapprovalId("pa-b");
        assinatura.setMpStatus("paused");
        assinatura.setMpPreapprovalSubstituidoId("pa-a");
        when(mercadoPagoClient.buscarPreapproval("pa-a")).thenReturn(noMp("pa-a", "authorized"));
        when(eventoRepository.findByTipoAndDataId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarLinkPagamento(assinatura))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo("PREAPPROVAL_SUBSTITUIDO_EM_ANALISE"));

        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        assertThat(assinatura.getMpPreapprovalId()).isEqualTo("pa-b");
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo("pa-a");
    }

    @Test
    @DisplayName("R3 - Cortesia antiga com preapproval: desvincula e cancela no MP quando ele não está authorized nem pago")
    void limpezaDeCortesiaCancelaPreapproval() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCortesia(true);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("paused");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(noMp(PREAPPROVAL_ID, "paused"));

        service.limparPreapprovalDeCortesia(assinatura);

        assertThat(assinatura.temPreapproval()).isFalse();
        verify(mercadoPagoClient).cancelarPreapproval(PREAPPROVAL_ID);
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isNull();
        verify(eventoRepository, never()).save(any());
    }

    @Test
    @DisplayName("R3 - Cortesia antiga com preapproval authorized: desvincula, não cancela e vira pendência manual")
    void limpezaDeCortesiaAutorizadaViraPendencia() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setCortesia(true);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        // Mesmo com o espelho local dizendo authorized: o status local da cortesia antiga não vale
        assinatura.setMpStatus("authorized");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(noMp(PREAPPROVAL_ID, "authorized"));
        when(eventoRepository.findByTipoAndDataId(any(), any())).thenReturn(Optional.empty());

        service.limparPreapprovalDeCortesia(assinatura);

        assertThat(assinatura.temPreapproval()).isFalse();
        assertThat(assinatura.getMpPreapprovalSubstituidoId()).isEqualTo(PREAPPROVAL_ID);
        verify(mercadoPagoClient, never()).cancelarPreapproval(any());
        verify(eventoRepository).save(any());
    }

    @Test
    @DisplayName("R3 - Limpeza ignora quem não é cortesia ou não tem preapproval")
    void limpezaDeCortesiaIgnoraNaoCortesia() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);

        service.limparPreapprovalDeCortesia(assinatura);

        assertThat(assinatura.getMpPreapprovalId()).isEqualTo(PREAPPROVAL_ID);
        verifyNoInteractions(mercadoPagoClient);
    }

    @Test
    @DisplayName("R7 - Pedido de link por evento (remoção da cortesia): gera o link e envia o e-mail")
    void eventoDeLinkGeraENotifica() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), any(), any(), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));
        when(assinaturaTenantRepository.findByMpPreapprovalId(PREAPPROVAL_ID)).thenReturn(Optional.of(assinatura));

        linkService.aoSolicitarLink(new LinkPagamentoSolicitadoEvent(ASSINATURA_ID));

        var dados = ArgumentCaptor.forClass(DadosEmailAtivacaoAssinatura.class);
        verify(emailNotificacaoService).enviarLinkAtivacaoAssinatura(dados.capture());
        assertThat(dados.getValue().email()).isEqualTo(EMAIL_ADMIN);
        assertThat(dados.getValue().initPoint()).isEqualTo(INIT_POINT);
    }

    @Test
    @DisplayName("R7 - Sem ciclo: o serviço do MP não depende do webhook e não usa @Lazy")
    void semDependenciaDoWebhook() {
        assertThat(java.util.Arrays.stream(AssinaturaMercadoPagoService.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType))
                .doesNotContain(MercadoPagoWebhookService.class);
        assertThat(java.util.Arrays.stream(AssinaturaMercadoPagoService.class.getConstructors())
                .flatMap(c -> java.util.Arrays.stream(c.getParameterAnnotations()))
                .flatMap(java.util.Arrays::stream)
                .map(java.lang.annotation.Annotation::annotationType))
                .doesNotContain(org.springframework.context.annotation.Lazy.class);
    }
}
