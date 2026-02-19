package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.presentation.dto.dashboard.AtualizarConfiguracaoGraficoRequest;
import br.com.saudeConecta.presentation.dto.dashboard.ConfiguracaoGraficoResponse;
import br.com.saudeConecta.service.ConfiguracaoGraficoDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard/configuracoes")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoGraficoDashboardController {

    private final ConfiguracaoGraficoDashboardService configuracaoService;

    @GetMapping
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> listarConfiguracoes() {
        log.debug("Listando todas as configurações de gráficos");
        List<ConfiguracaoGraficoResponse> configuracoes = configuracaoService.listarConfiguracoes();
        return ResponseEntity.ok(configuracoes);
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> listarGraficosAtivos() {
        log.debug("Listando gráficos ativos");
        List<ConfiguracaoGraficoResponse> graficosAtivos = configuracaoService.listarGraficosAtivos();
        return ResponseEntity.ok(graficosAtivos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_ORG')")
    public ResponseEntity<ConfiguracaoGraficoResponse> atualizarConfiguracao(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConfiguracaoGraficoRequest request) {
        log.info("Atualizando configuração de gráfico ID: {}", id);
        ConfiguracaoGraficoResponse response = configuracaoService.atualizarConfiguracao(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/batch")
    @PreAuthorize("hasRole('ADMIN_ORG')")
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> atualizarMultiplasConfiguracoes(
            @Valid @RequestBody List<AtualizarConfiguracaoGraficoRequest> requests) {
        log.info("Atualizando múltiplas configurações de gráficos");
        List<ConfiguracaoGraficoResponse> responses = configuracaoService.atualizarMultiplasConfiguracoes(requests);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/resetar")
    @PreAuthorize("hasRole('ADMIN_ORG')")
    public ResponseEntity<Void> resetarConfiguracoesParaPadrao() {
        log.info("Resetando configurações para padrão");
        configuracaoService.resetarConfiguracoesParaPadrao();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inicializar")
    @PreAuthorize("hasRole('ADMIN_ORG')")
    public ResponseEntity<List<ConfiguracaoGraficoResponse>> inicializarConfiguracoes() {
        log.info("Inicializando configurações de gráficos para primeira utilização");
        List<ConfiguracaoGraficoResponse> configuracoes = configuracaoService.inicializarConfiguracoesPrimeiroAcesso();
        return ResponseEntity.ok(configuracoes);
    }
}
