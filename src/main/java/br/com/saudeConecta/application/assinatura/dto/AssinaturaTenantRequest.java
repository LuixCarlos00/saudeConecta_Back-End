package br.com.saudeConecta.application.assinatura.dto;

import jakarta.validation.constraints.NotNull;

public record AssinaturaTenantRequest(

        @NotNull(message = "ID da organização é obrigatório")
        Long organizacaoId,

        @NotNull(message = "ID do plano é obrigatório")
        Long planoId
) {
}
