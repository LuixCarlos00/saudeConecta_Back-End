package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.planos.CobrancaTenantResponse;
import br.com.saudeConecta.service.CobrancaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cobrancas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class CobrancaController {

    private final CobrancaService cobrancaService;

    /**
     * Gera uma nova cobrança Pix para a assinatura (AdminOrg).
     */
    @PostMapping("/gerar/{assinaturaId}")
    public ResponseEntity<CobrancaTenantResponse> gerarCobranca(@PathVariable Long assinaturaId) {
        log.info("Gerando cobranca para assinatura ID: {}", assinaturaId);
        CobrancaTenantResponse response = cobrancaService.gerarCobranca(assinaturaId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Confirma pagamento manualmente (SuperAdmin).
     */
    @PatchMapping("/{cobrancaId}/confirmar-pagamento")
    public ResponseEntity<CobrancaTenantResponse> confirmarPagamento(@PathVariable Long cobrancaId) {
        log.info("Confirmacao manual de pagamento: cobrancaId={}", cobrancaId);
        CobrancaTenantResponse response = cobrancaService.confirmarPagamentoManual(cobrancaId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todas as cobranças pendentes (SuperAdmin).
     */
    @GetMapping("/pendentes")
    public ResponseEntity<List<CobrancaTenantResponse>> listarPendentes() {
        return ResponseEntity.ok(cobrancaService.listarTodasPendentes());
    }

    /**
     * Lista cobranças da organização do usuário logado (AdminOrg).
     */
    @GetMapping("/minhas-cobrancas")
    public ResponseEntity<List<CobrancaTenantResponse>> minhasCobrancas() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(cobrancaService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Busca cobrança pendente atual da organização (AdminOrg).
     * Usado para exibir alerta no dashboard.
     */
    @GetMapping("/pendente-atual")
    public ResponseEntity<CobrancaTenantResponse> buscarCobrancaPendenteAtual() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        CobrancaTenantResponse cobranca = cobrancaService.buscarCobrancaPendenteAtual(organizacaoId);
        
        if (cobranca == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(cobranca);
    }

    /**
     * [TESTE] Força geração de cobranças mensais (simula scheduler).
     * REMOVER EM PRODUÇÃO!
     */
    @PostMapping("/teste/gerar-cobrancas-mensais")
    public ResponseEntity<String> testarGeracaoCobrancas() {
        log.warn("[TESTE] Forçando geracao de cobranças mensais...");
        
        List<br.com.saudeConecta.domain.planos.AssinaturaTenant> assinaturas = 
            cobrancaService.listarAssinaturasAtivasParaCobranca();
        
        int geradas = 0;
        int erros = 0;
        
        for (var assinatura : assinaturas) {
            try {
                boolean temPendente = cobrancaService.existeCobrancaPendente(assinatura.getId());
                if (!temPendente) {
                    cobrancaService.gerarCobranca(assinatura.getId());
                    geradas++;
                } else {
                    log.info("[TESTE] Assinatura {} ja tem cobrança pendente", assinatura.getId());
                }
            } catch (Exception e) {
                erros++;
                log.error("[TESTE] Erro ao gerar cobranca: {}", e.getMessage());
            }
        }
        
        String resultado = String.format("Cobrancas geradas: %d | Erros: %d | Total: %d",
                geradas, erros, assinaturas.size());
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Lista cobranças de uma organização específica (SuperAdmin).
     */
    @GetMapping("/organizacao/{organizacaoId}")
    public ResponseEntity<List<CobrancaTenantResponse>> listarPorOrganizacao(
            @PathVariable Long organizacaoId) {
        return ResponseEntity.ok(cobrancaService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Busca cobrança pelo txid.
     */
    @GetMapping("/txid/{txid}")
    public ResponseEntity<CobrancaTenantResponse> buscarPorTxid(@PathVariable String txid) {
        return ResponseEntity.ok(cobrancaService.buscarPorTxid(txid));
    }
}
