package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

/**
 * Rotinas de ciclo de vida das assinaturas.
 *
 * Quem cobra é o Mercado Pago — o scheduler apenas prepara o link de ativação
 * antes do fim do trial, aplica a régua de carência e reconcilia os status
 * como rede de segurança contra webhooks perdidos.
 *
 * Assinaturas marcadas como cortesia são ignoradas por todas as rotinas
 * (o filtro vive nas próprias queries do repositório).
 *
 * Nenhuma rotina abre transação de lote: cada assinatura é processada em transação
 * própria (via {@link ProcessadorReguaCobranca} e
 * {@link AssinaturaMercadoPagoService#gerarLinkENotificar}), então a falha de uma
 * não desfaz nem interrompe as demais.
 */
@Component
public class CobrancaScheduler {

    private static final Logger log = LoggerFactory.getLogger(CobrancaScheduler.class);

    /** Antecedência, em dias, com que o link de pagamento é enviado antes do fim do trial. */
    private static final int DIAS_ANTECEDENCIA_LINK = 7;
    private static final int DIAS_CARENCIA_INADIMPLENCIA = 5;
    private static final int DIAS_CARENCIA_SUSPENSAO = 15;
    private static final int DIAS_CARENCIA_CANCELAMENTO = 30;

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    private final ProcessadorReguaCobranca processador;

    public CobrancaScheduler(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService,
            ProcessadorReguaCobranca processador) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
        this.processador = processador;
    }

    /**
     * Cria o preapproval no Mercado Pago e envia por e-mail o link do checkout
     * para os trials que vencem em até 7 dias e ainda não têm link
     * (inclui os que ficaram para trás por falha em execuções anteriores).
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void gerarLinksDeAtivacaoDeTrial() {
        LocalDate dataAlvo = LocalDate.now().plusDays(DIAS_ANTECEDENCIA_LINK);
        List<AssinaturaTenant> trials = assinaturaTenantRepository.findTrialsSemLinkVencendoAte(dataAlvo);
        if (trials.isEmpty()) {
            return;
        }
        log.info("[SCHEDULER] {} trial(s) vencendo até {} — gerando links de pagamento", trials.size(), dataAlvo);

        int gerados = processarCadaUm(trials, "gerar link de pagamento",
                id -> assinaturaMercadoPagoService.gerarLinkENotificar(id) != null);
        log.info("[SCHEDULER] Links de pagamento gerados: {}/{}", gerados, trials.size());
    }

    /**
     * Aplica a régua de carência: +5 dias vencida → INADIMPLENTE,
     * +15 dias → SUSPENSA (pausada no Mercado Pago), +30 dias suspensa → CANCELADA.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void aplicarReguaDeCarencia() {
        List<AssinaturaTenant> vencidas = assinaturaTenantRepository.findVencidasNaoInadimplentes(
                LocalDate.now().minusDays(DIAS_CARENCIA_INADIMPLENCIA));
        int marcadas = processarCadaUm(vencidas, "marcar inadimplente", id -> {
            processador.marcarInadimplente(id);
            return true;
        });

        List<AssinaturaTenant> inadimplentes = assinaturaTenantRepository.findInadimplentesParaSuspender(
                LocalDate.now().minusDays(DIAS_CARENCIA_SUSPENSAO));
        int suspensas = processarCadaUm(inadimplentes, "suspender", id -> {
            processador.suspender(id);
            return true;
        });

        LocalDateTime limiteCancelamento = LocalDate.now().minusDays(DIAS_CARENCIA_CANCELAMENTO).atStartOfDay();
        List<AssinaturaTenant> suspensasAntigas = assinaturaTenantRepository.findSuspensasParaCancelar(limiteCancelamento);
        int canceladas = processarCadaUm(suspensasAntigas, "cancelar", id -> {
            processador.cancelar(id);
            return true;
        });

        if (marcadas + suspensas + canceladas > 0) {
            log.warn("[SCHEDULER] Régua de carência: {} INADIMPLENTE(s), {} SUSPENSA(s), {} CANCELADA(s)",
                    marcadas, suspensas, canceladas);
        }
    }

    /**
     * Reconcilia o status local com o Mercado Pago a cada 4 horas.
     * Rede de segurança para notificações que nunca chegaram.
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void reconciliarComMercadoPago() {
        List<AssinaturaTenant> assinaturas = assinaturaTenantRepository.findComPreapprovalParaReconciliar();
        if (assinaturas.isEmpty()) {
            return;
        }

        int divergentes = processarCadaUm(assinaturas, "reconciliar", processador::sincronizar);
        log.info("[SCHEDULER] Reconciliação Mercado Pago: {} divergência(s) corrigida(s) em {} assinatura(s)",
                divergentes, assinaturas.size());
    }

    /**
     * Aplica a ação a cada assinatura isoladamente: a ação roda em transação própria
     * e qualquer erro é registrado e não impede as próximas.
     *
     * @return quantas assinaturas tiveram a ação concluída com resultado positivo
     */
    private int processarCadaUm(List<AssinaturaTenant> assinaturas, String acao, Predicate<Long> acaoPorId) {
        int sucesso = 0;
        for (AssinaturaTenant assinatura : assinaturas) {
            try {
                if (acaoPorId.test(assinatura.getId())) {
                    sucesso++;
                }
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao {} a assinatura {}", acao, assinatura.getId(), e);
            }
        }
        return sucesso;
    }
}
