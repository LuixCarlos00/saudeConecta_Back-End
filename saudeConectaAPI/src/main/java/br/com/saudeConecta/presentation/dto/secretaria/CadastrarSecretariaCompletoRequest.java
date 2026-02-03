package br.com.saudeConecta.presentation.dto.secretaria;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarSecretariaCompletoRequest(
        @NotBlank(message = "Nome não pode ser vazio")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String secreNome,
        
        @NotBlank(message = "Email não pode ser vazio")
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String secreEmail,
        
        @NotBlank(message = "CPF não pode ser vazio")
        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String secreCpf,
        
        @Size(max = 50, message = "Código de autorização deve ter no máximo 50 caracteres")
        String secreCodigoAutorizacao
) {
}
