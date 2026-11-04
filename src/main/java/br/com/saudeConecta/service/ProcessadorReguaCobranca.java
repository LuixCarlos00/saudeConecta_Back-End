package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executa cada passo da régua de cobrança em uma transação própria.
 *
 * Fica em um bean separado do {@link CobrancaScheduler} para que o proxy transacional
 * seja aplicado (chamada interna não passaria pelo proxy). Assim uma falha em uma
 * assinatura desfaz apenas aquela assinatura, e não o lote inteiro.
 */
@Component
public class ProcessadorReguaCobranca {

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
     * Marca a assinatura vencida como INADIMPLENTE.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarInadimplente(Long assinaturaId) {
        assinaturaTenantService.marcarInadimplente(assinaturaId);
    }

    /**
     * Suspende a assinatura e pausa a cobrança no Mercado Pago, na mesma transação.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void suspender(Long assinaturaId) {
        assinaturaTenantService.suspender(assinaturaId);
        assinaturaMercadoPagoService.pausar(buscar(assinaturaId));
    }

    /**
     * Cancela a assinatura suspensa há mais tempo que a carência, inclusive no Mercado Pago.
     *
     * @param assinaturaId ID da assinatura
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelar(Long assinaturaId) {
        assinaturaMercadoPagoService.cancelar(buscar(assinaturaId), OrigemCancelamento.SISTEMA);
    }

    /**
     * Reconcilia a assinatura com o Mercado Pago.
     *
     * @param assinaturaId ID da assinatura
     * @return true se houve divergência corrigida
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sincronizar(Long assinaturaId) {
        return assinaturaMercadoPagoService.sincronizar(buscar(assinaturaId));
    }

    private AssinaturaTenant buscar(Long assinaturaId) {
        return assinaturaTenantRepository.findById(assinaturaId)
                .orElseThrow(() -> new BusinessException(
                        "Assinatura não encontrada: " + assinaturaId, HttpStatus.NOT_FOUND));
    }
}
