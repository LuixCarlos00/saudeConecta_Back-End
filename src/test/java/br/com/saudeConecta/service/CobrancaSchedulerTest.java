package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import br.com.saudeConecta.service.mercadopago.MercadoPagoWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CobrancaScheduler - Testes unitários")
class CobrancaSchedulerTest {

    private static final Long ASSINATURA_ID = 42L;
    private static final Long OUTRA_ID = 43L;

    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    @Mock private ProcessadorReguaCobranca processador;
    @Mock private MercadoPagoWebhookService webhookService;

    private CobrancaScheduler scheduler;
    private AssinaturaTenant assinatura;
    private AssinaturaTenant outra;

    @BeforeEach
    void setUp() {
        scheduler = new CobrancaScheduler(assinaturaTenantRepository, assinaturaMercadoPagoService, processador,
                webhookService);

        Organizacao org = new Organizacao();
        org.setId(7L);
        PlanoAssinatura plano = new PlanoAssinatura();
        plano.setId(2L);

        assinatura = criar(ASSINATURA_ID, org, plano);
        outra = criar(OUTRA_ID, org, plano);

        when(assinaturaTenantRepository.findTrialsSemLinkVencendoAte(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findVencidasNaoInadimplentes(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of());
        when(assinaturaTenantRepository.findComPreapprovalParaReconciliar()).thenReturn(List.of());
    }

    private AssinaturaTenant criar(Long id, Organizacao org, PlanoAssinatura plano) {
        return AssinaturaTenant.builder()
                .id(id)
                .organizacao(org)
                .planoAssinatura(plano)
                .status(StatusAssinatura.TRIAL)
                .valorMensal(BigDecimal.valueOf(279))
                .dataVencimento(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("2.7 - Deve gerar o link para trials que vencem até hoje+7 (inclui quem ficou para trás)")
    void deveGerarLinkAteSeteDiasAntesDoFimDoTrial() {
        when(assinaturaTenantRepository.findTrialsSemLinkVencendoAte(LocalDate.now().plusDays(7)))
                .thenReturn(List.of(assinatura));

        scheduler.gerarLinksDeAtivacaoDeTrial();

        verify(assinaturaMercadoPagoService).gerarLinkENotificar(ASSINATURA_ID);
    }

    @Test
    @DisplayName("Sem trials na janela, nenhum link é gerado")
    void semTrialsNaJanela() {
        scheduler.gerarLinksDeAtivacaoDeTrial();

        verifyNoInteractions(assinaturaMercadoPagoService);
    }

    @Test
    @DisplayName("2.5 - Falha ao gerar link de uma assinatura não interrompe as demais")
    void falhaEmUmLinkNaoInterrompeAsDemais() {
        when(assinaturaTenantRepository.findTrialsSemLinkVencendoAte(any())).thenReturn(List.of(assinatura, outra));
        when(assinaturaMercadoPagoService.gerarLinkENotificar(ASSINATURA_ID))
                .thenThrow(new BusinessException("Mercado Pago fora do ar", HttpStatus.BAD_GATEWAY));

        scheduler.gerarLinksDeAtivacaoDeTrial();

        verify(assinaturaMercadoPagoService).gerarLinkENotificar(OUTRA_ID);
    }

    @Test
    @DisplayName("Régua de carência delega cada transição ao processador, item a item")
    void reguaDeCarenciaDelegaAoProcessador() {
        when(assinaturaTenantRepository.findVencidasNaoInadimplentes(any())).thenReturn(List.of(assinatura));
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of(assinatura));
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of(assinatura));

        scheduler.aplicarReguaDeCarencia();

        verify(processador).marcarInadimplente(ASSINATURA_ID, LocalDate.now().minusDays(5));
        verify(processador).suspender(ASSINATURA_ID, LocalDate.now().minusDays(15));
        verify(processador).cancelar(ASSINATURA_ID, LocalDate.now().minusDays(30).atStartOfDay());
    }

    @Test
    @DisplayName("H1c - Antes de suspender/cancelar, fatura aprovada não registrada no MP é recuperada e o item é pulado")
    void reguaRecuperaFaturaAprovadaAntesDeSuspenderOuCancelar() {
        assinatura.setMpPreapprovalId("pa-1");
        outra.setMpPreapprovalId("pa-2");
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of(assinatura));
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of(outra));
        when(webhookService.recuperarFaturasAprovadas("pa-1")).thenReturn(1);
        when(webhookService.recuperarFaturasAprovadas("pa-2")).thenReturn(1);

        scheduler.aplicarReguaDeCarencia();

        verify(processador, never()).suspender(anyLong(), any());
        verify(processador, never()).cancelar(anyLong(), any());
    }

    @Test
    @DisplayName("H1c - Sem fatura aprovada no MP a régua segue; sem preapproval nem consulta o MP")
    void reguaSegueQuandoNaoHaFaturaAprovada() {
        assinatura.setMpPreapprovalId("pa-1");
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of(assinatura, outra));
        when(webhookService.recuperarFaturasAprovadas("pa-1")).thenReturn(0);

        scheduler.aplicarReguaDeCarencia();

        verify(processador).suspender(eq(ASSINATURA_ID), any());
        verify(processador).suspender(eq(OUTRA_ID), any());
        verify(webhookService).recuperarFaturasAprovadas("pa-1");
        verifyNoMoreInteractions(webhookService);
    }

    @Test
    @DisplayName("H1c - MP fora do ar: suspensão (reversível) segue; cancelamento (terminal) fica para o próximo ciclo")
    void falhaNoMpSuspendeMasNaoCancela() {
        assinatura.setMpPreapprovalId("pa-1");
        outra.setMpPreapprovalId("pa-2");
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of(assinatura));
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of(outra));
        when(webhookService.recuperarFaturasAprovadas(anyString()))
                .thenThrow(new BusinessException("MP fora", HttpStatus.BAD_GATEWAY));

        scheduler.aplicarReguaDeCarencia();

        verify(processador).suspender(eq(ASSINATURA_ID), any());
        verify(processador, never()).cancelar(anyLong(), any());
    }

    @Test
    @DisplayName("2.5 - BusinessException em um item não desfaz nem interrompe os demais itens da régua")
    void erroEmUmItemDaReguaNaoInterrompeOsDemais() {
        when(assinaturaTenantRepository.findVencidasNaoInadimplentes(any())).thenReturn(List.of(assinatura, outra));
        when(assinaturaTenantRepository.findInadimplentesParaSuspender(any())).thenReturn(List.of(assinatura, outra));
        when(assinaturaTenantRepository.findSuspensasParaCancelar(any())).thenReturn(List.of(assinatura, outra));
        doThrow(new BusinessException("falha", HttpStatus.CONFLICT)).when(processador)
                .marcarInadimplente(eq(ASSINATURA_ID), any());
        doThrow(new ObjectOptimisticLockingFailureException(AssinaturaTenant.class, ASSINATURA_ID))
                .when(processador).suspender(eq(ASSINATURA_ID), any());
        doThrow(new BusinessException("falha", HttpStatus.BAD_GATEWAY)).when(processador)
                .cancelar(eq(ASSINATURA_ID), any());

        scheduler.aplicarReguaDeCarencia();

        verify(processador).marcarInadimplente(eq(OUTRA_ID), any());
        verify(processador).suspender(eq(OUTRA_ID), any());
        verify(processador).cancelar(eq(OUTRA_ID), any());
    }

    @Test
    @DisplayName("Reconciliação sincroniza item a item e segue após falha")
    void reconciliacaoSincronizaItemAItem() {
        when(assinaturaTenantRepository.findComPreapprovalParaReconciliar()).thenReturn(List.of(assinatura, outra));
        when(processador.sincronizar(ASSINATURA_ID)).thenThrow(new RuntimeException("timeout"));

        scheduler.reconciliarComMercadoPago();

        verify(processador).sincronizar(OUTRA_ID);
    }

    @Test
    @DisplayName("H1b - Reconciliação recupera faturas aprovadas sem webhook antes de sincronizar o preapproval")
    void reconciliacaoRecuperaFaturasAntesDeSincronizar() {
        assinatura.setMpPreapprovalId("pa-1");
        outra.setMpPreapprovalId("pa-2");
        when(assinaturaTenantRepository.findComPreapprovalParaReconciliar()).thenReturn(List.of(assinatura, outra));
        when(webhookService.recuperarFaturasAprovadas("pa-1")).thenThrow(new RuntimeException("timeout"));

        scheduler.reconciliarComMercadoPago();

        var ordem = inOrder(webhookService, processador);
        ordem.verify(webhookService).recuperarFaturasAprovadas("pa-1");
        ordem.verify(processador).sincronizar(ASSINATURA_ID);
        ordem.verify(webhookService).recuperarFaturasAprovadas("pa-2");
        ordem.verify(processador).sincronizar(OUTRA_ID);
    }

    @Test
    @DisplayName("H1a - Job de reprocessamento delega ao serviço de webhook")
    void reprocessaWebhooksComErro() {
        scheduler.reprocessarWebhooksComErro();

        verify(webhookService).reprocessarEventosComErro();
    }

    @Test
    @DisplayName("2.5 - O scheduler não abre transação de lote; o processador abre uma transação nova por item")
    void transacaoPorItem() throws NoSuchMethodException {
        for (Method metodo : CobrancaScheduler.class.getDeclaredMethods()) {
            assertThat(metodo.isAnnotationPresent(Transactional.class))
                    .as("CobrancaScheduler.%s não pode ter @Transactional de lote", metodo.getName())
                    .isFalse();
        }

        List<String> porItem = List.of("marcarInadimplente", "suspender", "cancelar", "sincronizar");
        Arrays.stream(ProcessadorReguaCobranca.class.getDeclaredMethods())
                .filter(m -> porItem.contains(m.getName()))
                .forEach(m -> assertThat(m.getAnnotation(Transactional.class).propagation())
                        .as("ProcessadorReguaCobranca.%s", m.getName())
                        .isEqualTo(Propagation.REQUIRES_NEW));
        assertThat(AssinaturaMercadoPagoService.class.getMethod("gerarLinkENotificar", Long.class)
                .getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
