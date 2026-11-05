package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.planos.*;
import br.com.saudeConecta.service.AssinaturaTenantService;
import br.com.saudeConecta.service.CobrancaService;
import br.com.saudeConecta.service.LimitePlanoService;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import br.com.saudeConecta.service.mercadopago.MercadoPagoWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assinaturas")
@RequiredArgsConstructor
@Slf4j
public class AssinaturaTenantController {

    private final AssinaturaTenantService assinaturaTenantService;
    private final AssinaturaMercadoPagoService assinaturaMercadoPagoService;
    private final LimitePlanoService limitePlanoService;
    private final CobrancaService cobrancaService;
    private final MercadoPagoWebhookService mercadoPagoWebhookService;

    /**
     * Cria nova assinatura (TRIAL) para a organização (SUPER_ADMIN).
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AssinaturaTenantResponse> assinar(
            @RequestBody @Valid AssinaturaTenantRequest request) {
        AssinaturaTenantResponse response = assinaturaTenantService.assinar(
                request.organizacaoId(), request.planoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Troca o plano da assinatura ativa da organização logada.
     */
    @PutMapping("/trocar-plano/{novoPlanoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssinaturaTenantResponse> trocarPlano(@PathVariable Long novoPlanoId) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(assinaturaTenantService.trocarPlano(organizacaoId, novoPlanoId));
    }

    /**
     * Busca a assinatura vigente (inclusive SUSPENSA) da organização do usuário logado.
     */
    @GetMapping("/minha-assinatura")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<AssinaturaTenantResponse> minhaAssinatura() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(assinaturaTenantService.buscarAssinaturaVigente(organizacaoId));
    }

    /**
     * Gera o link do checkout hospedado do Mercado Pago para ativar a assinatura.
     * O e-mail retornado precisa ser o da conta Mercado Pago do pagador.
     */
    @PostMapping("/minha-assinatura/gerar-link-pagamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LinkPagamentoResponse> gerarLinkPagamento() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(assinaturaMercadoPagoService.gerarLinkPagamento(organizacaoId));
    }

    /**
     * Cancela a assinatura da própria organização (cancelamento pelo cliente).
     */
    @DeleteMapping("/minha-assinatura")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelarMinhaAssinatura() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        assinaturaMercadoPagoService.cancelarPeloCliente(organizacaoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Histórico de cobranças da organização do usuário logado.
     */
    @GetMapping("/minha-assinatura/historico")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<CobrancaTenantResponse>> historico() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(cobrancaService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Retorna os limites de uso do plano da organização do usuário logado.
     */
    @GetMapping("/limites")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<LimitesPlanoResponse> limites() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(limitePlanoService.obterLimitesPlano(organizacaoId));
    }

    /**
     * Lista todas as assinaturas (SUPER_ADMIN).
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AssinaturaTenantResponse>> listarTodas() {
        return ResponseEntity.ok(assinaturaTenantService.listarTodas());
    }

    /**
     * Lista os eventos do Mercado Pago que precisam de tratamento manual (SUPER_ADMIN):
     * pagamento aprovado em assinatura cancelada e eventos com erro que expiraram.
     */
    @GetMapping("/pendencias-manuais")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<PendenciaManualResponse>> listarPendenciasManuais() {
        return ResponseEntity.ok(mercadoPagoWebhookService.listarPendenciasManuais());
    }

    /**
     * Lista assinaturas de uma organização específica (SUPER_ADMIN).
     */
    @GetMapping("/organizacao/{organizacaoId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AssinaturaTenantResponse>> listarPorOrganizacao(
            @PathVariable Long organizacaoId) {
        return ResponseEntity.ok(assinaturaTenantService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Customiza os limites do plano para um tenant específico (SUPER_ADMIN).
     * Permite adicionar perfis extras com cobrança adicional.
     */
    @PutMapping("/organizacao/{organizacaoId}/customizar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AssinaturaTenantResponse> customizarPlano(
            @PathVariable Long organizacaoId,
            @RequestBody @Valid CustomizarPlanoTenantRequest request) {
        return ResponseEntity.ok(assinaturaTenantService.customizarPlano(organizacaoId, request));
    }

    /**
     * Marca ou remove a condição de cortesia da assinatura (SUPER_ADMIN).
     * Assinaturas de cortesia não são cobradas nem suspensas pelo scheduler.
     */
    @PatchMapping("/{id}/cortesia")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AssinaturaTenantResponse> definirCortesia(
            @PathVariable Long id, @RequestParam boolean cortesia) {
        return ResponseEntity.ok(assinaturaTenantService.definirCortesia(id, cortesia));
    }

    /**
     * Suspende uma assinatura (SUPER_ADMIN).
     */
    @PatchMapping("/{id}/suspender")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> suspender(@PathVariable Long id) {
        assinaturaTenantService.suspender(id);
        assinaturaMercadoPagoService.pausar(assinaturaTenantService.buscarEntidadePorId(id));
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancela uma assinatura (SUPER_ADMIN), propagando o cancelamento ao Mercado Pago.
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        assinaturaMercadoPagoService.cancelar(
                assinaturaTenantService.buscarEntidadePorId(id), OrigemCancelamento.SUPER_ADMIN);
        return ResponseEntity.noContent().build();
    }
}
