package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.presentation.dto.dashboard.AtualizarConfiguracaoCardRequest;
import br.com.saudeConecta.presentation.dto.dashboard.ConfiguracaoCardResponse;
import br.com.saudeConecta.service.ConfiguracaoCardDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard/configuracoes-cards")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoCardDashboardController {

    private final ConfiguracaoCardDashboardService cardService;
    private final TenantHelper tenantHelper;

    @GetMapping
    public ResponseEntity<List<ConfiguracaoCardResponse>> listarConfiguracoes() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(cardService.listarConfiguracoes(usuarioId));
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ConfiguracaoCardResponse>> listarCardsAtivos() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(cardService.listarCardsAtivos(usuarioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracaoCardResponse> atualizarConfiguracao(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConfiguracaoCardRequest request) {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(cardService.atualizarConfiguracao(id, request, usuarioId));
    }

    @PutMapping("/batch")
    public ResponseEntity<List<ConfiguracaoCardResponse>> atualizarMultiplasConfiguracoes(
            @Valid @RequestBody List<AtualizarConfiguracaoCardRequest> requests) {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(cardService.atualizarMultiplasConfiguracoes(requests, usuarioId));
    }

    @PostMapping("/resetar")
    public ResponseEntity<Void> resetarConfiguracoesParaPadrao() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        cardService.resetarConfiguracoesParaPadrao(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inicializar")
    public ResponseEntity<List<ConfiguracaoCardResponse>> inicializarConfiguracoes() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(cardService.inicializarConfiguracoesPrimeiroAcesso(usuarioId));
    }
}
