package br.com.saudeConecta.presentation.dto.secretaria;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AtualizarSecretariaRequest(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String secreNome,
        
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String secreEmail,
        
        @Size(max = 50, message = "Código de autorização deve ter no máximo 50 caracteres")
        String secreCodigoAutorizacao
) {
}
