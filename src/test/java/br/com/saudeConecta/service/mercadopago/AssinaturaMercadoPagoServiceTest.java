package br.com.saudeConecta.service.mercadopago;

import static org.assertj.core.api.Assertions.assertThatCode;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.LinkPagamentoResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private MercadoPagoProperties properties;
    private AssinaturaMercadoPagoService service;
    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setAccessToken("token-teste");
        properties.setBackUrl("https://app.saudeconecta.com.br/assinatura/sucesso");

        service = new AssinaturaMercadoPagoService(assinaturaTenantRepository,
                adminOrganizacaoRepository, mercadoPagoClient, properties, emailNotificacaoService);

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
        when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(ORG_ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaTenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Deve criar preapproval e persistir o link de pagamento")
    void deveCriarPreapprovalEPersistirLink() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = service.gerarLinkPagamento(ORG_ID);

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

        LinkPagamentoResponse link = service.gerarLinkPagamento(ORG_ID);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(mercadoPagoClient, never()).criarPreapprovalPendente(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Não deve gerar link para assinatura de cortesia")
    void naoDeveGerarLinkParaCortesia() {
        assinatura.setCortesia(true);

        assertThatThrownBy(() -> service.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cortesia");
    }

    @Test
    @DisplayName("Não deve gerar link para assinatura já ativa no Mercado Pago")
    void naoDeveGerarLinkParaAssinaturaAtiva() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);

        assertThatThrownBy(() -> service.gerarLinkPagamento(ORG_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está ativa");
    }

    @Test
    @DisplayName("Deve falhar quando a organização não tem e-mail de administrador")
    void deveFalharSemEmailDeAdministrador() {
        when(adminOrganizacaoRepository.findOwnerByOrganizacaoId(ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarLinkPagamento(ORG_ID))
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
    @DisplayName("Gerar link e notificar: cria o preapproval e envia o e-mail ao administrador")
    void gerarLinkENotificar() {
        when(assinaturaTenantRepository.findById(ASSINATURA_ID)).thenReturn(Optional.of(assinatura));
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null, null));

        LinkPagamentoResponse link = service.gerarLinkENotificar(ASSINATURA_ID);

        assertThat(link.initPoint()).isEqualTo(INIT_POINT);
        verify(emailNotificacaoService).enviarLinkAtivacaoAssinatura(
                eq(EMAIL_ADMIN), eq("Fulano"), eq("Profissional"), any(), eq(INIT_POINT),
                eq(EMAIL_ADMIN), any(), eq(ORG_ID), eq(3L));
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
}
