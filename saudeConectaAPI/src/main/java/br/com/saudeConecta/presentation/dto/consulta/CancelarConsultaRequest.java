package br.com.saudeConecta.presentation.dto.consulta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelarConsultaRequest(
    @NotNull(message = "Informar quem está cancelando")
    String canceladoPor,
    
    @NotBlank(message = "Motivo do cancelamento é obrigatório")
    String motivo
) {}
