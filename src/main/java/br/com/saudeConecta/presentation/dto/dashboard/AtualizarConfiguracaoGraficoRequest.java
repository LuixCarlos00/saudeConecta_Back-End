package br.com.saudeConecta.presentation.dto.dashboard;

import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;
import jakarta.validation.constraints.NotNull;

public record AtualizarConfiguracaoGraficoRequest(
    @NotNull(message = "Tipo de gráfico é obrigatório")
    TipoGraficoDashboard tipoGrafico,
    
    @NotNull(message = "Status ativo é obrigatório")
    Boolean ativo,
    
    Integer ordemExibicao
) {}
