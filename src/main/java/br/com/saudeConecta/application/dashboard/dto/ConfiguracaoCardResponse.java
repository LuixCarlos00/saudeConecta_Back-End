package br.com.saudeConecta.application.dashboard.dto;

import br.com.saudeConecta.domain.dashboard.TipoCardDashboard;

public record ConfiguracaoCardResponse(
    Long id,
    TipoCardDashboard tipoCard,
    String descricao,
    Boolean ativo,
    Integer ordemExibicao
) {}
