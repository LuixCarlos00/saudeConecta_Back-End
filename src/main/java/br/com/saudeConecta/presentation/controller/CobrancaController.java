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
import java.util.Map;

@RestController
@RequestMapping("/cobrancas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class CobrancaController {

    private final CobrancaService cobrancaService;

    /**
     * Gera uma nova cobrança Pix para a assinatura.
     */
    @PostMapping("/gerar/{assinaturaId}")
    public ResponseEntity<CobrancaTenantResponse> gerarCobranca(@PathVariable Long assinaturaId) {
        log.info("Gerando cobrança para assinatura ID: {}", assinaturaId);
        CobrancaTenantResponse response = cobrancaService.gerarCobranca(assinaturaId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Webhook para confirmação de pagamento Pix pelo PSP.
     * Acesso público (autenticação via assinatura do PSP).
     */
    @PostMapping("/webhook/pix")
    public ResponseEntity<Void> webhookPix(@RequestBody Map<String, Object> payload) {
        String txid = extrairTxidDoPayload(payload);
        if (txid == null) {
            log.warn("Webhook Pix recebido sem txid válido: {}", payload);
            return ResponseEntity.badRequest().build();
        }

        log.info("Webhook Pix recebido: txid={}", txid);
        cobrancaService.confirmarPagamento(txid);
        return ResponseEntity.ok().build();
    }

    /**
     * Lista cobranças da organização do usuário logado.
     */
    @GetMapping("/minhas-cobrancas")
    public ResponseEntity<List<CobrancaTenantResponse>> minhasCobrancas() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(cobrancaService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Lista cobranças de uma organização específica (SUPER_ADMIN).
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

    /**
     * Extrai o txid do payload do webhook Pix.
     * Formato genérico — adaptar conforme PSP (EfiPay, Mercado Pago, etc.).
     */
    @SuppressWarnings("unchecked")
    private String extrairTxidDoPayload(Map<String, Object> payload) {
        if (payload.containsKey("txid")) {
            return String.valueOf(payload.get("txid"));
        }

        if (payload.containsKey("pix")) {
            Object pixObj = payload.get("pix");
            if (pixObj instanceof List<?> pixList && !pixList.isEmpty()) {
                Object primeiro = pixList.get(0);
                if (primeiro instanceof Map<?, ?> pixMap) {
                    return String.valueOf(pixMap.get("txid"));
                }
            }
        }

        return null;
    }
}
