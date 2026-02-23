package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.presentation.dto.dashboard.AtualizarConfiguracaoGraficoRequest;
import br.com.saudeConecta.presentation.dto.dashboard.ConfiguracaoGraficoResponse;
import br.com.saudeConecta.service.ConfiguracaoGraficoDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard/configuracoes")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoGraficoDashboardController {

    private final ConfiguracaoGraficoDashboardService configuracaoService;
    private final TenantHelper tenantHelper;

    @GetMapping
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> listarConfiguracoes() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(configuracaoService.listarConfiguracoes(usuarioId));
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> listarGraficosAtivos() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(configuracaoService.listarGraficosAtivos(usuarioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracaoGraficoResponse> atualizarConfiguracao(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConfiguracaoGraficoRequest request) {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(configuracaoService.atualizarConfiguracao(id, request, usuarioId));
    }

    @PutMapping("/batch")
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> atualizarMultiplasConfiguracoes(
            @Valid @RequestBody List<AtualizarConfiguracaoGraficoRequest> requests) {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(configuracaoService.atualizarMultiplasConfiguracoes(requests, usuarioId));
    }

    @PostMapping("/resetar")
    public ResponseEntity<Void> resetarConfiguracoesParaPadrao() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        configuracaoService.resetarConfiguracoesParaPadrao(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inicializar")
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> inicializarConfiguracoes() {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return ResponseEntity.ok(configuracaoService.inicializarConfiguracoesPrimeiroAcesso(usuarioId));
    }
}