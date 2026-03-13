package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class CobrancaScheduler {

    private static final Logger log = LoggerFactory.getLogger(CobrancaScheduler.class);

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AssinaturaTenantService assinaturaTenantService;
    private final CobrancaService cobrancaService;

    public CobrancaScheduler(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AssinaturaTenantService assinaturaTenantService,
            CobrancaService cobrancaService) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaTenantService = assinaturaTenantService;
        this.cobrancaService = cobrancaService;
    }

    /**
     * Gera cobranças mensais automaticamente no dia 1º de cada mês às 08:00.
     * Cria cobrança para todas as assinaturas ATIVAS e TRIAL.
     * Vencimento: dia 10 do mês (10 dias para pagamento).
     */
    @Scheduled(cron = "0 0 8 1 * *")
    @Transactional
    public void gerarCobrancasMensaisAutomaticas() {
        log.info("[SCHEDULER] Iniciando geração automática de cobranças mensais (dia 1º do mês)...");
        
        List<AssinaturaTenant> assinaturas = assinaturaTenantRepository
                .findByStatusIn(List.of(
                    br.com.saudeConecta.domain.planos.StatusAssinatura.ATIVA,
                    br.com.saudeConecta.domain.planos.StatusAssinatura.TRIAL
                ));

        int geradas = 0;
        int erros = 0;
        
        for (AssinaturaTenant assinatura : assinaturas) {
            try {
                // Verifica se já existe cobrança pendente para evitar duplicação
                boolean temPendente = cobrancaService.existeCobrancaPendente(assinatura.getId());
                if (!temPendente) {
                    cobrancaService.gerarCobranca(assinatura.getId());
                    geradas++;
                    log.info("[SCHEDULER] Cobrança gerada para organização: {}", 
                            assinatura.getOrganizacao().getNome());
                } else {
                    log.debug("[SCHEDULER] Assinatura {} já possui cobrança pendente, ignorando", 
                            assinatura.getId());
                }
            } catch (Exception e) {
                erros++;
                log.error("[SCHEDULER] Erro ao gerar cobrança para assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        log.info("[SCHEDULER] Cobranças mensais geradas: {} | Erros: {} | Total processado: {}", 
                geradas, erros, assinaturas.size());
    }

    /**
     * Expira cobranças Pix com data de vencimento ultrapassada.
     * Executa a cada 6 horas.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void expirarPixVencidos() {
        log.info("[SCHEDULER] Verificando Pix vencidos...");
        int expiradas = cobrancaService.expirarCobrancasVencidas();
        if (expiradas > 0) {
            log.info("[SCHEDULER] {} cobranças Pix expiradas", expiradas);
        }
    }

    /**
     * Marca assinaturas vencidas há mais de 5 dias como INADIMPLENTE.
     * Executa diariamente às 09:00.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void marcarInadimplentes() {
        log.info("[SCHEDULER] Verificando assinaturas para marcar como inadimplentes...");
        LocalDate limite = LocalDate.now().minusDays(5);
        List<AssinaturaTenant> vencidas = assinaturaTenantRepository
                .findVencidasNaoInadimplentes(limite);

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

    /**
     * Suspende assinaturas inadimplentes há mais de 15 dias.
     * Executa diariamente às 10:00.
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void suspenderInadimplentes() {
        log.info("[SCHEDULER] Verificando assinaturas para suspender...");
        LocalDate limite = LocalDate.now().minusDays(15);
        List<AssinaturaTenant> inadimplentes = assinaturaTenantRepository
                .findInadimplentesParaSuspender(limite);

        int suspensas = 0;
        for (AssinaturaTenant assinatura : inadimplentes) {
            try {
                assinaturaTenantService.suspender(assinatura.getId());
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

    /**
     * Cancela assinaturas suspensas há mais de 30 dias.
     * Executa diariamente às 11:00.
     */
    @Scheduled(cron = "0 0 11 * * *")
    @Transactional
    public void cancelarSuspensas() {
        log.info("[SCHEDULER] Verificando assinaturas para cancelar...");
        LocalDate limite = LocalDate.now().minusDays(30);
        List<AssinaturaTenant> suspensas = assinaturaTenantRepository
                .findSuspensasParaCancelar(limite);

        int canceladas = 0;
        for (AssinaturaTenant assinatura : suspensas) {
            try {
                assinaturaTenantService.cancelar(assinatura.getId());
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
}
