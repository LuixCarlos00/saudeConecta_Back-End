package br.com.saudeConecta.e2e;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.planos.*;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.CobrancaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.MpWebhookEventoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanoAssinaturaRepository;
import br.com.saudeConecta.service.CobrancaScheduler;
import br.com.saudeConecta.service.mercadopago.MercadoPagoClient;
import br.com.saudeConecta.service.mercadopago.MercadoPagoWebhookService;
import br.com.saudeConecta.service.mercadopago.dto.AuthorizedPaymentResponse;
import br.com.saudeConecta.service.mercadopago.dto.PreapprovalResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Ponta a ponta da cobrança SaaS com banco real (H2) e transações reais.
 * Só o transporte HTTP do Mercado Pago (MercadoPagoClient) e o e-mail são simulados.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
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
                inv.getArgument(0), "authorized", null, null, null, null));

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
                PREAPPROVAL_ID, "authorized", null, null, "ASSINATURA-" + trial.getId(), null));

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
}
