package br.com.saudeConecta.presentation.dto.dashboard;

import br.com.saudeConecta.domain.dashboard.TipoCardDashboard;
import jakarta.validation.constraints.NotNull;

public record AtualizarConfiguracaoCardRequest(
    @NotNull(message = "Tipo de card é obrigatório")
    TipoCardDashboard tipoCard,

    @NotNull(message = "Status ativo é obrigatório")
    Boolean ativo,

    Integer ordemExibicao
) {}
