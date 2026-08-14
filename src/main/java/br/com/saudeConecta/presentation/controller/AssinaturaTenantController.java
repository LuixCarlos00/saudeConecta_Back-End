package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.planos.AssinaturaTenantRequest;
import br.com.saudeConecta.presentation.dto.planos.AssinaturaTenantResponse;
import br.com.saudeConecta.presentation.dto.planos.CustomizarPlanoTenantRequest;
import br.com.saudeConecta.presentation.dto.planos.LimitesPlanoResponse;
import br.com.saudeConecta.service.AssinaturaTenantService;
import br.com.saudeConecta.service.LimitePlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assinaturas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AssinaturaTenantController {

    private final AssinaturaTenantService assinaturaTenantService;
    private final LimitePlanoService limitePlanoService;

    /**
     * Cria nova assinatura (TRIAL) para a organização.
     */
    @PostMapping
    public ResponseEntity<AssinaturaTenantResponse> assinar(
            @RequestBody @Valid AssinaturaTenantRequest request) {
        log.info("Nova assinatura: org={}, plano={}", request.organizacaoId(), request.planoId());
        AssinaturaTenantResponse response = assinaturaTenantService.assinar(
                request.organizacaoId(), request.planoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Troca o plano da assinatura ativa.
     */
    @PutMapping("/trocar-plano/{novoPlanoId}")
    public ResponseEntity<AssinaturaTenantResponse> trocarPlano(@PathVariable Long novoPlanoId) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        log.info("Troca de plano: org={}, novoPlano={}", organizacaoId, novoPlanoId);
        return ResponseEntity.ok(assinaturaTenantService.trocarPlano(organizacaoId, novoPlanoId));
    }

    /**
     * Busca a assinatura ativa da organização do usuário logado.
     */
    @GetMapping("/minha-assinatura")
    public ResponseEntity<AssinaturaTenantResponse> minhaAssinatura() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(assinaturaTenantService.buscarAssinaturaAtiva(organizacaoId));
    }

    /**
     * Retorna os limites de uso do plano da organização do usuário logado.
     */
    @GetMapping("/limites")
    public ResponseEntity<LimitesPlanoResponse> limites() {
        Long organizacaoId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(limitePlanoService.obterLimitesPlano(organizacaoId));
    }

    /**
     * Lista todas as assinaturas (SUPER_ADMIN).
     */
    @GetMapping
    public ResponseEntity<List<AssinaturaTenantResponse>> listarTodas() {
        return ResponseEntity.ok(assinaturaTenantService.listarTodas());
    }

    /**
     * Lista assinaturas de uma organização específica (SUPER_ADMIN).
     */
    @GetMapping("/organizacao/{organizacaoId}")
    public ResponseEntity<List<AssinaturaTenantResponse>> listarPorOrganizacao(
            @PathVariable Long organizacaoId) {
        return ResponseEntity.ok(assinaturaTenantService.listarPorOrganizacao(organizacaoId));
    }

    /**
     * Customiza os limites do plano para um tenant específico (SUPER_ADMIN).
     * Permite adicionar perfis extras com cobrança adicional.
     */
    @PutMapping("/organizacao/{organizacaoId}/customizar")
    public ResponseEntity<AssinaturaTenantResponse> customizarPlano(
            @PathVariable Long organizacaoId,
            @RequestBody @Valid CustomizarPlanoTenantRequest request) {
        log.info("SUPER_ADMIN customizando plano: org={}, limAdmin={}, limProf={}, limSec={}",
                organizacaoId, request.limiteAdminOrgCustom(),
                request.limiteProfissionalCustom(), request.limiteSecretariaCustom());
        return ResponseEntity.ok(assinaturaTenantService.customizarPlano(organizacaoId, request));
    }

    /**
     * Suspende uma assinatura (SUPER_ADMIN).
     */
    @PatchMapping("/{id}/suspender")
    public ResponseEntity<Void> suspender(@PathVariable Long id) {
        log.warn("Suspendendo assinatura ID: {}", id);
        assinaturaTenantService.suspender(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancela uma assinatura (SUPER_ADMIN).
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        log.warn("Cancelando assinatura ID: {}", id);
        assinaturaTenantService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
