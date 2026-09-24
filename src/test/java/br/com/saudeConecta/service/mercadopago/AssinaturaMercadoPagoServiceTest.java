package br.com.saudeConecta.service.mercadopago;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
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

    private MercadoPagoProperties properties;
    private AssinaturaMercadoPagoService service;
    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setAccessToken("token-teste");
        properties.setBackUrl("https://app.saudeconecta.com.br/assinatura/sucesso");

        service = new AssinaturaMercadoPagoService(assinaturaTenantRepository,
                adminOrganizacaoRepository, mercadoPagoClient, properties);

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

        when(adminOrganizacaoRepository.findOwnerByOrganizacaoId(ORG_ID)).thenReturn(Optional.of(admin));
        when(assinaturaTenantRepository.findAssinaturaAtivaByOrganizacaoId(ORG_ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaTenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Deve criar preapproval e persistir o link de pagamento")
    void deveCriarPreapprovalEPersistirLink() {
        when(mercadoPagoClient.criarPreapprovalPendente(any(), eq("ASSINATURA-42"), eq(EMAIL_ADMIN), any()))
                .thenReturn(new PreapprovalResponse(PREAPPROVAL_ID, "pending", INIT_POINT,
                        EMAIL_ADMIN, "ASSINATURA-42", null));

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
                new PreapprovalResponse(PREAPPROVAL_ID, "cancelled", null, EMAIL_ADMIN, null, null));

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
    @DisplayName("Sincronizar deve corrigir divergência de status vinda do Mercado Pago")
    void sincronizarDeveCorrigirDivergencia() {
        assinatura.setMpPreapprovalId(PREAPPROVAL_ID);
        assinatura.setMpStatus("pending");
        when(mercadoPagoClient.buscarPreapproval(PREAPPROVAL_ID)).thenReturn(
                new PreapprovalResponse(PREAPPROVAL_ID, "authorized", INIT_POINT, EMAIL_ADMIN, null, null));

        boolean mudou = service.sincronizar(assinatura);

        assertThat(mudou).isTrue();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
    }

    @Test
    @DisplayName("Mapeamento de status do Mercado Pago para status interno")
    void deveMapearStatus() {
        assertThat(service.mapearStatus("authorized")).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(service.mapearStatus("paused")).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(service.mapearStatus("cancelled")).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(service.mapearStatus("pending")).isNull();
        assertThat(service.mapearStatus(null)).isNull();
    }
}
