package br.com.saudeConecta.service.mercadopago;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.service.AssinaturaTenantService;
import br.com.saudeConecta.service.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MercadoPagoWebhookService - Testes unitários")
class MercadoPagoWebhookServiceTest {

    private static final String PREAPPROVAL_ID = "2c93808497f9d3a80197fa1234567890";
    private static final String AUTHORIZED_PAYMENT_ID = "9876543210";
    private static final Long ASSINATURA_ID = 42L;

    @Mock private MpWebhookEventoRepository eventoRepository;
    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private CobrancaTenantRepository cobrancaTenantRepository;
    @Mock private AssinaturaTenantService assinaturaTenantService;
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    @Mock private PlatformTransactionManager transactionManager;

    private MercadoPagoWebhookService service;
    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoWebhookService(eventoRepository, assinaturaTenantRepository,
                cobrancaTenantRepository, assinaturaTenantService, mercadoPagoClient,
                assinaturaMercadoPagoService, transactionManager);

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
        when(assinaturaMercadoPagoService.mapearStatus("authorized")).thenReturn(StatusAssinatura.ATIVA);
        when(assinaturaMercadoPagoService.mapearStatus("cancelled")).thenReturn(StatusAssinatura.CANCELADA);
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
    @DisplayName("Preapproval authorized deve ativar a assinatura e renovar o vencimento")
    void preapprovalAuthorizedDeveAtivarAssinatura() {
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", "https://mp/checkout",
                        "admin@clinica.com", "ASSINATURA-42", null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getMpStatus()).isEqualTo("authorized");
        assertThat(assinatura.getDataVencimento()).isEqualTo(java.time.LocalDate.now().plusMonths(1));
        verify(assinaturaTenantRepository).save(assinatura);
    }

    @Test
    @DisplayName("Preapproval cancelled deve cancelar a assinatura registrando a origem")
    void preapprovalCancelledDeveCancelarAssinatura() {
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null,
                        "admin@clinica.com", "ASSINATURA-42", null));

        service.processar(MercadoPagoWebhookService.TOPICO_PREAPPROVAL, PREAPPROVAL_ID, "{}");

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.getCanceladoPor()).isEqualTo(OrigemCancelamento.MERCADO_PAGO);
        assertThat(assinatura.getDataCancelamento()).isNotNull();
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
        assertThat(cobranca.getFormaPagamento()).isEqualTo(FormaPagamento.MERCADO_PAGO);
        assertThat(cobranca.getMpAuthorizedPaymentId()).isEqualTo(AUTHORIZED_PAYMENT_ID);
        assertThat(cobranca.getValorTotal()).isEqualByComparingTo(BigDecimal.valueOf(279));
        verify(assinaturaTenantService).renovarAposPagamento(ASSINATURA_ID);
    }

    @Test
    @DisplayName("Pagamento recusado deve registrar cobrança RECUSADO e marcar inadimplência")
    void pagamentoRecusadoDeveMarcarInadimplente() {
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
}
