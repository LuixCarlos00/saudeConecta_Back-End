package br.com.saudeConecta.presentation.dto.suporte;

import br.com.saudeConecta.domain.suporte.StatusChamado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para atualizacao do status de um chamado de suporte.
 *
 * @param status    novo status do chamado
 * @param observacao mensagem opcional enviada ao autor do chamado
 */
public record AtualizarStatusChamadoRequest(

        @NotNull(message = "O status e obrigatorio")
        StatusChamado status,

        @Size(max = 1000, message = "A observacao deve ter no maximo 1000 caracteres")
        String observacao
) {
}
