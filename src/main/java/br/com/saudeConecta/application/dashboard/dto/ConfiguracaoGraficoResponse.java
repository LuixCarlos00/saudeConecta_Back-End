package br.com.saudeConecta.application.dashboard.dto;

import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;

public record ConfiguracaoGraficoResponse(
    Long id,
    TipoGraficoDashboard tipoGrafico,
    String descricao,
    Boolean ativo,
    Integer ordemExibicao
) {}
