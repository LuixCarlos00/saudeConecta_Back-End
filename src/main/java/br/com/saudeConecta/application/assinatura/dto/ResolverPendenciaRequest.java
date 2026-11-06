package br.com.saudeConecta.application.assinatura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PATCH /assinaturas/pendencias-manuais/{id}/resolver.
 *
 * @param observacao o que o SUPER_ADMIN fez para resolver (estorno, religação...), obrigatória
 */
public record ResolverPendenciaRequest(
        @NotBlank(message = "Informe a observação da resolução")
        @Size(max = 500, message = "A observação pode ter no máximo 500 caracteres")
        String observacao
) {
}
