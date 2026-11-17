package br.com.saudeConecta.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da redefinição de senha pelo link.
 *
 * @param token     token recebido no link do e-mail
 * @param novaSenha nova senha, entre 8 e 100 caracteres
 */
public record DadosRedefinirSenha(
        @NotBlank(message = "Link inválido") @Size(max = 200, message = "Link inválido") String token,
        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres") String novaSenha) {
}
