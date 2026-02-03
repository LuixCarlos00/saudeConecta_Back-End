package br.com.saudeConecta.presentation.dto.secretaria;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.sql.Date;

public record CadastrarSecretariaRequest(
        @NotBlank(message = "Nome não pode ser vazio")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String SecreNome,
        
        @NotNull(message = "Status não pode ser nulo")
        Byte SecreStatus,
        
        @JsonFormat(pattern = "dd/MM/yyyy")
        Date SecreDataCriacao,
        
        @NotBlank(message = "Email não pode ser vazio")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String SecreEmail,
        
        @Size(max = 50, message = "Código de autorização deve ter no máximo 50 caracteres")
        String SecreCodigoAtorizacao,
        
        @NotNull(message = "ID do usuário não pode ser nulo")
        @JsonProperty("SecreUsuario")
        Long secreUsuario
) {
}
