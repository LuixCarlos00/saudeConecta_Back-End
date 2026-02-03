package br.com.saudeConecta.presentation.dto.administrador;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastrarAdministradorRequest(
        @NotBlank(message = "Nome não pode ser vazio")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String admNome,
        
        @NotNull(message = "Status não pode ser nulo")
        Byte admStatus,
        
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate admDataCriacao,
        
        @NotBlank(message = "Email não pode ser vazio")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String admEmail,
        
        @Size(max = 50, message = "Código de autorização deve ter no máximo 50 caracteres")
        String admCodigoAtorizacao,
        
        @NotNull(message = "ID do usuário não pode ser nulo")
        @JsonProperty("AdmUsuario")
        Long admUsuario
) {
}
