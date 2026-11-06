package br.com.saudeConecta.presentation.controller.cobranca;

import br.com.saudeConecta.application.assinatura.dto.CobrancaTenantResponse;
import br.com.saudeConecta.application.cobranca.CobrancaService;
import br.com.saudeConecta.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cobrancas")
@RequiredArgsConstructor
@Slf4j
public class CobrancaController {

    private final CobrancaService cobrancaService;

    /**
     * Confirma pagamento manualmente (SUPER_ADMIN).
     * Usado apenas em conciliações excepcionais — o fluxo normal é automático
     * via webhook do Mercado Pago.
     */
    @PatchMapping("/{cobrancaId}/confirmar-pagamento")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CobrancaTenantResponse> confirmarPagamento(@PathVariable Long cobrancaId) {
        return ResponseEntity.ok(cobrancaService.confirmarPagamentoManual(cobrancaId));
    }

    /**
     * Lista todas as cobranças pendentes (SUPER_ADMIN).
     */
    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CobrancaTenantResponse>> listarPendentes() {
        return ResponseEntity.ok(cobrancaService.listarTodasPendentes());
    }

    /**
     * Lista cobranças da organização do usuário logado (AdminOrg).
     */
    @GetMapping("/minhas-cobrancas")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<CobrancaTenantResponse>> minhasCobrancas() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(cobrancaService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Busca cobrança pendente atual da organização (AdminOrg).
     * Usado para exibir alerta no dashboard.
     */
    @GetMapping("/pendente-atual")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<CobrancaTenantResponse> buscarCobrancaPendenteAtual() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        CobrancaTenantResponse cobranca = cobrancaService.buscarCobrancaPendenteAtual(organizacaoId);

        if (cobranca == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(cobranca);
    }

    /**
     * Lista cobranças de uma organização específica (SUPER_ADMIN).
     */
    @GetMapping("/organizacao/{organizacaoId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CobrancaTenantResponse>> listarPorOrganizacao(
            @PathVariable Long organizacaoId) {
        return ResponseEntity.ok(cobrancaService.listarPorOrganizacao(organizacaoId));
    }
}
