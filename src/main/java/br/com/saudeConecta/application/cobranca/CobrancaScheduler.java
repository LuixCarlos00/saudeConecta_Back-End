package br.com.saudeConecta.application.cobranca;

import br.com.saudeConecta.application.assinatura.AssinaturaMercadoPagoService;
import br.com.saudeConecta.application.assinatura.LinkPagamentoAssinaturaService;
import br.com.saudeConecta.application.assinatura.MercadoPagoWebhookService;
import br.com.saudeConecta.config.FusoHorarioConfig;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

/**
 * Rotinas de ciclo de vida das assinaturas.
 *
 * Quem cobra é o Mercado Pago — o scheduler apenas prepara o link de ativação
 * antes do fim do trial, aplica a régua de carência, reconcilia os status
 * e reprocessa webhooks que falharam, como rede de segurança contra pagamentos
 * aprovados que não chegaram a ser registrados.
 *
 * Assinaturas marcadas como cortesia são ignoradas por todas as rotinas
 * (o filtro vive nas próprias queries do repositório), exceto a limpeza da cortesia
 * antiga que ainda tem preapproval vinculado, feita na reconciliação.
 *
 * Nenhuma rotina abre transação de lote: cada assinatura é processada em transação
 * própria (via {@link ProcessadorReguaCobranca} e
 * {@link LinkPagamentoAssinaturaService#gerarLinkENotificar}), então a falha de uma
 * não desfaz nem interrompe as demais.
 */
@Component
public class CobrancaScheduler {
    private static final String LOG_REGUA = "cobranca.regua";

    private static final Logger log = LoggerFactory.getLogger(CobrancaScheduler.class);

    /** Antecedência, em dias, com que o link de pagamento é enviado antes do fim do trial. */
    private static final int DIAS_ANTECEDENCIA_LINK = 7;
    private static final int DIAS_CARENCIA_INADIMPLENCIA = 5;
    private static final int DIAS_CARENCIA_SUSPENSAO = 15;
    private static final int DIAS_CARENCIA_CANCELAMENTO = 30;

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    private final LinkPagamentoAssinaturaService linkPagamentoAssinaturaService;
    private final ProcessadorReguaCobranca processador;
    private final MercadoPagoWebhookService webhookService;
    /** Relógio em America/Sao_Paulo: as datas-limite da régua usam o "hoje" de Brasília. */
    private final Clock clock;

    public CobrancaScheduler(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService,
            LinkPagamentoAssinaturaService linkPagamentoAssinaturaService,
            ProcessadorReguaCobranca processador,
            MercadoPagoWebhookService webhookService,
            Clock clock) {
        this.clock = clock;
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
        this.linkPagamentoAssinaturaService = linkPagamentoAssinaturaService;
        this.processador = processador;
        this.webhookService = webhookService;
    }

    /**
     * Cria o preapproval no Mercado Pago e envia por e-mail o link do checkout
     * para os trials que vencem em até 7 dias e ainda não têm link
     * (inclui os que ficaram para trás por falha em execuções anteriores).
     */
    @Scheduled(cron = "0 0 8 * * *", zone = FusoHorarioConfig.FUSO)
    public void gerarLinksDeAtivacaoDeTrial() {
        LocalDate dataAlvo = LocalDate.now(clock).plusDays(DIAS_ANTECEDENCIA_LINK);
        List<AssinaturaTenant> trials = assinaturaTenantRepository.findTrialsSemLinkVencendoAte(dataAlvo);
        if (trials.isEmpty()) {
            return;
        }
        log.info("cobranca.links inicio trials={} vencimentoAte={}", trials.size(), dataAlvo);

        int gerados = processarCadaUm(trials, "cobranca.links", "gerar link de pagamento",
                assinatura -> linkPagamentoAssinaturaService.gerarLinkENotificar(assinatura.getId()) != null);
        log.info("cobranca.links ok processados={} falhas={}", gerados, trials.size() - gerados);
    }

    /**
     * Aplica a régua de carência: +5 dias vencida → INADIMPLENTE,
     * +15 dias → SUSPENSA (pausada no Mercado Pago), +30 dias suspensa → CANCELADA.
     * Antes de suspender ou cancelar, confere no MP se há fatura aprovada não registrada.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = FusoHorarioConfig.FUSO)
    public void aplicarReguaDeCarencia() {
        LocalDate limiteInadimplencia = LocalDate.now(clock).minusDays(DIAS_CARENCIA_INADIMPLENCIA);
        List<AssinaturaTenant> vencidas = assinaturaTenantRepository.findVencidasNaoInadimplentes(limiteInadimplencia);
        int marcadas = processarCadaUm(vencidas, LOG_REGUA, "marcar inadimplente",
                assinatura -> processador.marcarInadimplente(assinatura.getId(), limiteInadimplencia));

        LocalDate limiteSuspensao = LocalDate.now(clock).minusDays(DIAS_CARENCIA_SUSPENSAO);
        List<AssinaturaTenant> inadimplentes = assinaturaTenantRepository.findInadimplentesParaSuspender(limiteSuspensao);
        int suspensas = processarCadaUm(inadimplentes, LOG_REGUA, "suspender", assinatura ->
                !pagamentoAprovadoRecuperado(assinatura, true)
                        && processador.suspender(assinatura.getId(), limiteSuspensao));

        LocalDateTime limiteCancelamento = LocalDate.now(clock).minusDays(DIAS_CARENCIA_CANCELAMENTO).atStartOfDay();
        List<AssinaturaTenant> suspensasAntigas = assinaturaTenantRepository.findSuspensasParaCancelar(limiteCancelamento);
        int canceladas = processarCadaUm(suspensasAntigas, LOG_REGUA, "cancelar", assinatura ->
                !pagamentoAprovadoRecuperado(assinatura, false)
                        && processador.cancelar(assinatura.getId(), limiteCancelamento));

        if (marcadas + suspensas + canceladas > 0) {
            log.info("cobranca.regua ok inadimplentes={} suspensas={} canceladas={}",
                    marcadas, suspensas, canceladas);
        }
    }

    /**
     * Reconcilia o status local com o Mercado Pago a cada 4 horas.
     * Rede de segurança para notificações que nunca chegaram.
     */
    @Scheduled(cron = "0 0 */4 * * *", zone = FusoHorarioConfig.FUSO)
    public void reconciliarComMercadoPago() {
        cancelarPreapprovalsSubstituidos();
        limparCortesiasComPreapproval();

        List<AssinaturaTenant> assinaturas = assinaturaTenantRepository.findComPreapprovalParaReconciliar();
        if (assinaturas.isEmpty()) {
            return;
        }

        int divergentes = processarCadaUm(assinaturas, "cobranca.reconciliar", "reconciliar", assinatura -> {
            // Faturas primeiro: a renovação por pagamento aprovado reativa um preapproval pausado,
            // e a sincronização seguinte já enxerga o status atualizado.
            recuperarFaturasAprovadas(assinatura);
            return processador.sincronizar(assinatura.getId());
        });
        log.info("cobranca.reconciliar ok processadas={} divergentes={}",
                assinaturas.size(), divergentes);
    }

    /**
     * Nova tentativa de cancelar no MP os preapprovals trocados cujo cancelamento falhou
     * depois do commit da troca (link novo, cortesia, troca de plano com link pendente).
     * Sem isso o antigo poderia continuar cobrando em paralelo ao novo.
     */
    private void cancelarPreapprovalsSubstituidos() {
        List<Long> pendentes = assinaturaTenantRepository.findIdsComPreapprovalSubstituidoPendente();
        if (pendentes.isEmpty()) {
            return;
        }
        int cancelados = 0;
        for (Long assinaturaId : pendentes) {
            try {
                if (assinaturaMercadoPagoService.cancelarPreapprovalSubstituido(assinaturaId)) {
                    cancelados++;
                }
            } catch (Exception e) {
                log.error("cobranca.reconciliar falha acao=cancelar preapproval substituido assinaturaId={}",
                        assinaturaId, e);
            }
        }
        log.info("cobranca.reconciliar substituidos pendentes={} cancelados={}", pendentes.size(), cancelados);
    }

    /**
     * Cortesias antigas (de antes de a cortesia desvincular o preapproval) com preapproval vivo
     * continuam cobrando no MP. Cada uma é desvinculada e o preapproval é cancelado depois do
     * commit; authorized ou com fatura aprovada sem registro vira pendência manual.
     */
    private void limparCortesiasComPreapproval() {
        List<Long> cortesias = assinaturaTenantRepository.findIdsCortesiaComPreapproval();
        if (cortesias.isEmpty()) {
            return;
        }
        int desvinculadas = 0;
        for (Long assinaturaId : cortesias) {
            try {
                if (processador.limparPreapprovalDeCortesia(assinaturaId)) {
                    desvinculadas++;
                }
            } catch (Exception e) {
                log.error("assinatura.cortesia.limpeza falha assinaturaId={}", assinaturaId, e);
            }
        }
        log.info("assinatura.cortesia.limpeza ok encontradas={} desvinculadas={}", cortesias.size(), desvinculadas);
    }

    /**
     * Reprocessa, a cada 15 minutos, as notificações do Mercado Pago cujo processamento
     * falhou (ex.: GET da fatura com 5xx/timeout). Sem isso um pagamento aprovado cuja
     * notificação falhou nunca seria registrado. Backoff e expiração ficam no serviço.
     */
    @Scheduled(cron = "0 */15 * * * *", zone = FusoHorarioConfig.FUSO)
    public void reprocessarWebhooksComErro() {
        int reprocessados = webhookService.reprocessarEventosComErro();
        if (reprocessados > 0) {
            log.info("mp.webhook.reprocessar ok reprocessados={}", reprocessados);
        }
    }

    /**
     * Antes de suspender ou cancelar, confere no Mercado Pago se existe fatura aprovada
     * que não chegou a ser registrada (webhook perdido) e, se existir, registra e renova.
     *
     * @param prosseguirSeMpFalhar comportamento quando o MP não responde: a suspensão é
     *                             reversível (um pagamento reativa) e segue; o cancelamento
     *                             é terminal e fica para o próximo ciclo
     * @return true quando o item deve ser pulado (pagamento recuperado, ou MP fora do ar
     *         em ação terminal)
     */
    private boolean pagamentoAprovadoRecuperado(AssinaturaTenant assinatura, boolean prosseguirSeMpFalhar) {
        if (!assinatura.temPreapproval()) {
            return false;
        }
        try {
            int recuperadas = webhookService.recuperarFaturasAprovadas(assinatura.getMpPreapprovalId());
            if (recuperadas > 0) {
                log.warn("cobranca.regua ignorado motivo=FATURA_APROVADA_SEM_REGISTRO assinaturaId={} recuperadas={}",
                        assinatura.getId(), recuperadas);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("cobranca.regua falha motivo=MP_INDISPONIVEL assinaturaId={} proximo={}",
                    assinatura.getId(),
                    prosseguirSeMpFalhar ? "SEGUE_COM_REGUA" : "ADIADO_PROXIMO_CICLO", e);
            return !prosseguirSeMpFalhar;
        }
    }

    /** Falha aqui não impede a sincronização do preapproval logo em seguida. */
    private void recuperarFaturasAprovadas(AssinaturaTenant assinatura) {
        try {
            webhookService.recuperarFaturasAprovadas(assinatura.getMpPreapprovalId());
        } catch (Exception e) {
            log.warn("cobranca.reconciliar falha motivo=MP_INDISPONIVEL assinaturaId={}",
                    assinatura.getId(), e);
        }
    }

    /**
     * Aplica a ação a cada assinatura isoladamente: a ação roda em transação própria
     * e qualquer erro é registrado e não impede as próximas.
     *
     * @param evento nome do evento de log da rotina chamadora (ex.: {@code cobranca.regua}),
     *               para a falha de um item aparecer sob o mesmo evento do resumo da rotina
     * @return quantas assinaturas tiveram a ação concluída com resultado positivo
     */
    private int processarCadaUm(List<AssinaturaTenant> assinaturas, String evento, String acao,
                                Predicate<AssinaturaTenant> acaoPorItem) {
        int sucesso = 0;
        for (AssinaturaTenant assinatura : assinaturas) {
            try {
                if (acaoPorItem.test(assinatura)) {
                    sucesso++;
                }
            } catch (OptimisticLockingFailureException e) {
                // Outra transação (webhook/admin) alterou a assinatura; o item volta no próximo ciclo
                log.warn("{} conflito.versao acao={} assinaturaId={} proximo=RETOMAR_NO_PROXIMO_CICLO",
                        evento, acao, assinatura.getId());
            } catch (Exception e) {
                log.error("{} falha acao={} assinaturaId={}", evento, acao, assinatura.getId(), e);
            }
        }
        return sucesso;
    }
}
