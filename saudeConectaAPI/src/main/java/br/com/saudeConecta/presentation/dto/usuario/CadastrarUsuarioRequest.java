package br.com.saudeConecta.presentation.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastrarUsuarioRequest(
        @NotBlank(message = "Login não pode ser vazio")
        @Size(max = 50, message = "Login deve ter no máximo 50 caracteres")
        String login,
        
        @NotBlank(message = "Senha não pode ser vazia")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String senha,
        
        @NotNull(message = "Tipo de usuário não pode ser nulo")
        Byte tipoUsuario,
        
        @NotNull(message = "Status não pode ser nulo")
        Byte status
) {
}
