package br.com.saudeConecta.presentation.dto.dashboard;

import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;

public record ConfiguracaoGraficoResponse(
    Long id,
    TipoGraficoDashboard tipoGrafico,
    String descricao,
    Boolean ativo,
    Integer ordemExibicao
) {}
