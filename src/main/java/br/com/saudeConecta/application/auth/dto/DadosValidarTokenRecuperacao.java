package br.com.saudeConecta.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da validação do link de redefinição. O token vai no corpo, não na URL da API, para não
 * aparecer em log de acesso.
 *
 * @param token token recebido no link do e-mail
 */
public record DadosValidarTokenRecuperacao(
        @NotBlank(message = "Link inválido") @Size(max = 200, message = "Link inválido") String token) {
}
