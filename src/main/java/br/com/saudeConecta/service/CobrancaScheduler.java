package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.presentation.dto.planos.LinkPagamentoResponse;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rotinas de ciclo de vida das assinaturas.
 *
 * Quem cobra é o Mercado Pago — o scheduler apenas prepara o link de ativação
 * antes do fim do trial, aplica a régua de carência e reconcilia os status
 * como rede de segurança contra webhooks perdidos.
 *
 * Assinaturas marcadas como cortesia são ignoradas por todas as rotinas
 * (o filtro vive nas próprias queries do repositório).
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
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final AssinaturaTenantService assinaturaTenantService;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    private final EmailNotificacaoService emailNotificacaoService;

    public CobrancaScheduler(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            AssinaturaTenantService assinaturaTenantService,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService,
            EmailNotificacaoService emailNotificacaoService) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.assinaturaTenantService = assinaturaTenantService;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
        this.emailNotificacaoService = emailNotificacaoService;
    }

    /**
     * Cria o preapproval no Mercado Pago e envia por e-mail o link do checkout
     * para os trials que vencem em 7 dias.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void gerarLinksDeAtivacaoDeTrial() {
        LocalDate dataAlvo = LocalDate.now().plusDays(DIAS_ANTECEDENCIA_LINK);
        List<AssinaturaTenant> trials = assinaturaTenantRepository.findTrialsSemLinkVencendoEm(dataAlvo);

        if (trials.isEmpty()) {
            return;
        }
        log.info("[SCHEDULER] {} trial(s) vencendo em {} — gerando links de pagamento", trials.size(), dataAlvo);

        int gerados = 0;
        for (AssinaturaTenant assinatura : trials) {
            try {
                LinkPagamentoResponse link = assinaturaMercadoPagoService.gerarLinkPagamento(assinatura);
                notificarLinkDePagamento(assinatura, link);
                gerados++;
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao gerar link de pagamento da assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        log.info("[SCHEDULER] Links de pagamento gerados: {}/{}", gerados, trials.size());
    }

    /**
     * Aplica a régua de carência: +5 dias vencida → INADIMPLENTE,
     * +15 dias → SUSPENSA (pausada no Mercado Pago), +30 dias → CANCELADA.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void aplicarReguaDeCarencia() {
        marcarInadimplentes();
        suspenderInadimplentes();
        cancelarSuspensas();
    }

    /**
     * Reconcilia o status local com o Mercado Pago a cada 4 horas.
     * Rede de segurança para notificações que nunca chegaram.
     */
    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void reconciliarComMercadoPago() {
        List<AssinaturaTenant> assinaturas = assinaturaTenantRepository.findComPreapprovalParaReconciliar();
        if (assinaturas.isEmpty()) {
            return;
        }

        int divergentes = 0;
        for (AssinaturaTenant assinatura : assinaturas) {
            try {
                if (assinaturaMercadoPagoService.sincronizar(assinatura)) {
                    divergentes++;
                }
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao reconciliar assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        log.info("[SCHEDULER] Reconciliação Mercado Pago: {} divergência(s) corrigida(s) em {} assinatura(s)",
                divergentes, assinaturas.size());
    }

    private void marcarInadimplentes() {
        LocalDate limite = LocalDate.now().minusDays(DIAS_CARENCIA_INADIMPLENCIA);
        List<AssinaturaTenant> vencidas = assinaturaTenantRepository.findVencidasNaoInadimplentes(limite);

        int marcadas = 0;
        for (AssinaturaTenant assinatura : vencidas) {
            try {
                assinaturaTenantService.marcarInadimplente(assinatura.getId());
                marcadas++;
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao marcar inadimplente assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        if (marcadas > 0) {
            log.warn("[SCHEDULER] {} assinaturas marcadas como INADIMPLENTE", marcadas);
        }
    }

    private void suspenderInadimplentes() {
        LocalDate limite = LocalDate.now().minusDays(DIAS_CARENCIA_SUSPENSAO);
        List<AssinaturaTenant> inadimplentes = assinaturaTenantRepository.findInadimplentesParaSuspender(limite);

        int suspensas = 0;
        for (AssinaturaTenant assinatura : inadimplentes) {
            try {
                assinaturaTenantService.suspender(assinatura.getId());
                assinaturaMercadoPagoService.pausar(assinatura);
                suspensas++;
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao suspender assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        if (suspensas > 0) {
            log.warn("[SCHEDULER] {} assinaturas SUSPENSAS", suspensas);
        }
    }

    private void cancelarSuspensas() {
        LocalDateTime limite = LocalDate.now().minusDays(DIAS_CARENCIA_CANCELAMENTO).atStartOfDay();
        List<AssinaturaTenant> suspensas = assinaturaTenantRepository.findSuspensasParaCancelar(limite);

        int canceladas = 0;
        for (AssinaturaTenant assinatura : suspensas) {
            try {
                assinaturaMercadoPagoService.cancelar(assinatura, OrigemCancelamento.SISTEMA);
                canceladas++;
            } catch (Exception e) {
                log.error("[SCHEDULER] Erro ao cancelar assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        if (canceladas > 0) {
            log.warn("[SCHEDULER] {} assinaturas CANCELADAS", canceladas);
        }
    }

    private void notificarLinkDePagamento(AssinaturaTenant assinatura, LinkPagamentoResponse link) {
        AdminOrganizacao owner = adminOrganizacaoRepository
                .findOwnerByOrganizacaoId(assinatura.getOrganizacao().getId())
                .orElse(null);

        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("[SCHEDULER] Assinatura {} sem administrador com e-mail — link não enviado",
                    assinatura.getId());
            return;
        }

        emailNotificacaoService.enviarLinkAtivacaoAssinatura(
                owner.getEmail(),
                owner.getNome(),
                assinatura.getPlanoAssinatura().getNome(),
                assinatura.getValorMensal(),
                link.initPoint(),
                link.payerEmail(),
                assinatura.getDataVencimento(),
                assinatura.getOrganizacao().getId(),
                owner.getId());
    }
}
