package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.LinkPagamentoResponse;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CobrancaScheduler - Testes unitários")
class CobrancaSchedulerTest {

    private static final Long ASSINATURA_ID = 42L;
    private static final String INIT_POINT = "https://mp/checkout";

    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private AssinaturaTenantService assinaturaTenantService;
    @Mock private AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    @Mock private EmailNotificacaoService emailNotificacaoService;

    private CobrancaScheduler scheduler;
    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        scheduler = new CobrancaScheduler(assinaturaTenantRepository, adminOrganizacaoRepository,
                assinaturaTenantService, assinaturaMercadoPagoService, emailNotificacaoService);

        Organizacao org = new Organizacao();
        org.setId(7L);
        org.setNome("Clínica Teste");

        PlanoAssinatura plano = new PlanoAssinatura();
        plano.setId(2L);
        plano.setNome("Profissional");

        assinatura = AssinaturaTenant.builder()
                .id(ASSINATURA_ID)
                .organizacao(org)
                .planoAssinatura(plano)
                .status(StatusAssinatura.TRIAL)
                .valorMensal(BigDecimal.valueOf(279))
                .dataVencimento(LocalDate.now().plusDays(7))
                .build();

        AdminOrganizacao admin = new AdminOrganizacao();
        admin.setId(3L);
        admin.setNome("Fulano");
        admin.setEmail("admin@clinica.com.br");
        when(adminOrganizacaoRepository.findOwnerByOrganizacaoId(7L)).thenReturn(Optional.of(admin));

        when(assinaturaTenantRepository.findTrialsSemLinkVencendoEm(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findVencidasNaoInadimplentes(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findComPreapprovalParaReconciliar()).thenReturn(List.of());
    }

    @Test
    @DisplayName("Deve gerar o link de ativação 7 dias antes do fim do trial e notificar por e-mail")
    void deveGerarLinkSeteDiasAntesDoFimDoTrial() {
        LocalDate alvo = LocalDate.now().plusDays(7);
        when(assinaturaTenantRepository.findTrialsSemLinkVencendoEm(alvo)).thenReturn(List.of(assinatura));
        when(assinaturaMercadoPagoService.gerarLinkPagamento(assinatura)).thenReturn(
                new LinkPagamentoResponse("pa-1", INIT_POINT, "admin@clinica.com.br", BigDecimal.valueOf(279)));

        scheduler.gerarLinksDeAtivacaoDeTrial();

        verify(assinaturaMercadoPagoService).gerarLinkPagamento(assinatura);
        verify(emailNotificacaoService).enviarLinkAtivacaoAssinatura(
                eq("admin@clinica.com.br"), eq("Fulano"), eq("Profissional"),
                any(), eq(INIT_POINT), eq("admin@clinica.com.br"), any(), eq(7L), eq(3L));
    }

    @Test
    @DisplayName("Não deve gerar cobrança para assinaturas em TRIAL fora da janela de ativação")
    void naoDeveCobrarTrialForaDaJanela() {
        scheduler.gerarLinksDeAtivacaoDeTrial();

        verify(assinaturaMercadoPagoService, never()).gerarLinkPagamento(any(AssinaturaTenant.class));
        verifyNoInteractions(emailNotificacaoService);
    }

    @Test
    @DisplayName("Falha ao gerar link de uma assinatura não deve interromper as demais")
    void falhaEmUmaAssinaturaNaoInterrompeAsDemais() {
        AssinaturaTenant outra = AssinaturaTenant.builder()
                .id(43L)
                .organizacao(assinatura.getOrganizacao())
                .planoAssinatura(assinatura.getPlanoAssinatura())
                .status(StatusAssinatura.TRIAL)
                .valorMensal(BigDecimal.valueOf(279))
                .dataVencimento(LocalDate.now().plusDays(7))
                .build();

        when(assinaturaTenantRepository.findTrialsSemLinkVencendoEm(any()))
                .thenReturn(List.of(assinatura, outra));
        when(assinaturaMercadoPagoService.gerarLinkPagamento(assinatura))
                .thenThrow(new RuntimeException("Mercado Pago fora do ar"));
        when(assinaturaMercadoPagoService.gerarLinkPagamento(outra)).thenReturn(
                new LinkPagamentoResponse("pa-2", INIT_POINT, "admin@clinica.com.br", BigDecimal.valueOf(279)));

        scheduler.gerarLinksDeAtivacaoDeTrial();

        verify(assinaturaMercadoPagoService).gerarLinkPagamento(outra);
        verify(emailNotificacaoService, times(1)).enviarLinkAtivacaoAssinatura(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Régua de carência deve marcar inadimplente, suspender (pausando no MP) e cancelar")
    void reguaDeCarenciaDeveAplicarTodasAsTransicoes() {
        AssinaturaTenant vencida = assinatura;
        when(assinaturaTenantRepository.findVencidasNaoInadimplentes(any())).thenReturn(List.of(vencida));
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of(vencida));
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of(vencida));

        scheduler.aplicarReguaDeCarencia();

        verify(assinaturaTenantService).marcarInadimplente(ASSINATURA_ID);
        verify(assinaturaTenantService).suspender(ASSINATURA_ID);
        verify(assinaturaMercadoPagoService).pausar(vencida);
        verify(assinaturaMercadoPagoService).cancelar(vencida, OrigemCancelamento.SISTEMA);
    }

    @Test
    @DisplayName("Reconciliação deve sincronizar todas as assinaturas com preapproval")
    void reconciliacaoDeveSincronizarAssinaturas() {
        assinatura.setMpPreapprovalId("pa-1");
        when(assinaturaTenantRepository.findComPreapprovalParaReconciliar()).thenReturn(List.of(assinatura));

        scheduler.reconciliarComMercadoPago();

        verify(assinaturaMercadoPagoService).sincronizar(assinatura);
    }
}
