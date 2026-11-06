package br.com.saudeConecta.application.cobranca;

import br.com.saudeConecta.application.assinatura.AssinaturaMercadoPagoService;
import br.com.saudeConecta.application.assinatura.AssinaturaTenantService;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Executa cada passo da régua de cobrança em uma transação própria.
 *
 * Fica em um bean separado do {@link CobrancaScheduler} para que o proxy transacional
 * seja aplicado (chamada interna não passaria pelo proxy). Assim uma falha em uma
 * assinatura desfaz apenas aquela assinatura, e não o lote inteiro.
 *
 * O lote é lido no início da rotina; até o item ser processado um webhook pode ter
 * renovado a assinatura. Por isso cada passo relê a linha com lock de escrita e repete
 * a mesma condição da query do lote: se ela não vale mais, o item é ignorado.
 */
@Component
public class ProcessadorReguaCobranca {
    private static final String MSG_ASSINATURA_NAO_ENCONTRADA = "Assinatura não encontrada: ";

    private static final Logger log = LoggerFactory.getLogger(ProcessadorReguaCobranca.class);

    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final AssinaturaTenantService assinaturaTenantService;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;

    public ProcessadorReguaCobranca(
            AssinaturaTenantRepository assinaturaTenantRepository,
            AssinaturaTenantService assinaturaTenantService,
            AssinaturaMercadoPagoService assinaturaMercadoPagoService) {
        this.assinaturaTenantRepository = assinaturaTenantRepository;
        this.assinaturaTenantService = assinaturaTenantService;
        this.assinaturaMercadoPagoService = assinaturaMercadoPagoService;
    }

    /**
     * Marca a assinatura vencida como INADIMPLENTE, se ainda for ATIVA/TRIAL vencida antes do limite.
     *
     * @param assinaturaId ID da assinatura
     * @param limite       mesmo limite de vencimento usado na busca do lote
     * @return true se a assinatura foi marcada; false se o item foi ignorado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean marcarInadimplente(Long assinaturaId, LocalDate limite) {
        if (!aindaValida(assinaturaId, "marcar inadimplente", a ->
                (StatusAssinatura.ATIVA.equals(a.getStatus()) || StatusAssinatura.TRIAL.equals(a.getStatus()))
                        && vencidaAntesDe(a, limite))) {
            return false;
        }
        assinaturaTenantService.marcarInadimplente(assinaturaId);
        return true;
    }

    /**
     * Suspende a assinatura e pausa a cobrança no Mercado Pago, na mesma transação,
     * se ela ainda for INADIMPLENTE vencida antes do limite.
     *
     * @param assinaturaId ID da assinatura
     * @param limite       mesmo limite de vencimento usado na busca do lote
     * @return true se a assinatura foi suspensa; false se o item foi ignorado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean suspender(Long assinaturaId, LocalDate limite) {
        if (!aindaValida(assinaturaId, "suspender", a ->
                StatusAssinatura.INADIMPLENTE.equals(a.getStatus()) && vencidaAntesDe(a, limite))) {
            return false;
        }
        assinaturaTenantService.suspender(assinaturaId);
        assinaturaMercadoPagoService.pausar(buscar(assinaturaId));
        return true;
    }

    /**
     * Cancela a assinatura suspensa há mais tempo que a carência, inclusive no Mercado Pago.
     *
     * @param assinaturaId ID da assinatura
     * @param limite       mesmo limite de data de suspensão usado na busca do lote
     * @return true se a assinatura foi cancelada; false se o item foi ignorado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancelar(Long assinaturaId, LocalDateTime limite) {
        if (!aindaValida(assinaturaId, "cancelar", a -> {
            // Mesmo COALESCE da query: linhas antigas sem data_suspensao usam atualizado_em
            LocalDateTime suspensaEm = a.getDataSuspensao() != null ? a.getDataSuspensao() : a.getAtualizadoEm();
            return StatusAssinatura.SUSPENSA.equals(a.getStatus()) && suspensaEm != null && suspensaEm.isBefore(limite);
        })) {
            return false;
        }
        assinaturaMercadoPagoService.cancelar(buscar(assinaturaId), OrigemCancelamento.SISTEMA);
        return true;
    }

    /**
     * Reconcilia a assinatura com o Mercado Pago, se ela ainda tiver preapproval e não estiver cancelada.
     *
     * @param assinaturaId ID da assinatura
     * @return true se houve divergência corrigida
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sincronizar(Long assinaturaId) {
        if (!aindaValida(assinaturaId, "reconciliar", a ->
                a.temPreapproval() && !StatusAssinatura.CANCELADA.equals(a.getStatus()))) {
            return false;
        }
        return assinaturaMercadoPagoService.sincronizar(buscar(assinaturaId));
    }

    /**
     * Cortesia antiga com preapproval vinculado: desvincula e agenda o cancelamento no MP para
     * depois do commit (com a regra de pendência manual para authorized ou pago). A linha é
     * relida travada para decidir sobre o estado atual.
     *
     * @param assinaturaId ID da assinatura
     * @return true se o preapproval foi desvinculado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean limparPreapprovalDeCortesia(Long assinaturaId) {
        AssinaturaTenant atual = assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        MSG_ASSINATURA_NAO_ENCONTRADA + assinaturaId, HttpStatus.NOT_FOUND));
        if (!atual.isCortesia() || !atual.temPreapproval()) {
            return false;
        }
        assinaturaMercadoPagoService.limparPreapprovalDeCortesia(atual);
        assinaturaTenantRepository.save(atual);
        return true;
    }

    /**
     * Relê a assinatura com PESSIMISTIC_WRITE (a linha fica travada até o fim da transação)
     * e confere a condição do lote. Cortesia sempre tira a assinatura da régua.
     */
    private boolean aindaValida(Long assinaturaId, String acao, Predicate<AssinaturaTenant> condicao) {
        AssinaturaTenant atual = assinaturaTenantRepository.findByIdParaAtualizacao(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        MSG_ASSINATURA_NAO_ENCONTRADA + assinaturaId, HttpStatus.NOT_FOUND));

        if (atual.isCortesia() || !condicao.test(atual)) {
            log.debug("cobranca.regua ignorado motivo=ESTADO_MUDOU acao={} assinaturaId={} status={}",
                    acao, assinaturaId, atual.getStatus());
            return false;
        }
        return true;
    }

    private static boolean vencidaAntesDe(AssinaturaTenant assinatura, LocalDate limite) {
        return assinatura.getDataVencimento() != null && assinatura.getDataVencimento().isBefore(limite);
    }

    /** Devolve a mesma instância já travada no contexto de persistência da transação. */
    private AssinaturaTenant buscar(Long assinaturaId) {
        return assinaturaTenantRepository.findById(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        MSG_ASSINATURA_NAO_ENCONTRADA + assinaturaId, HttpStatus.NOT_FOUND));
    }
}
