package br.com.saudeConecta.presentation.dto.especialidade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EspecialidadeUpdateRequest(
    @NotBlank(message = "O nome da especialidade é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String nome,
    
    @Size(max = 20, message = "O código deve ter no máximo 20 caracteres")
    String codigo,
    
    Byte status
) {
}
