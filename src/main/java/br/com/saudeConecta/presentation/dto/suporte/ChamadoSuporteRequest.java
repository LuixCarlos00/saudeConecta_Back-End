package br.com.saudeConecta.presentation.dto.suporte;

import br.com.saudeConecta.domain.suporte.CategoriaChamado;
import br.com.saudeConecta.domain.suporte.PrioridadeChamado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de entrada para abertura de um chamado de suporte.
 *
 * @param titulo     titulo resumido do chamado
 * @param corpo      descricao detalhada do problema ou solicitacao
 * @param categoria  categoria do chamado
 * @param prioridade prioridade informada pelo usuario
 * @param anexos     lista opcional de imagens anexadas
 */
public record ChamadoSuporteRequest(

        @NotBlank(message = "O titulo e obrigatorio")
        @Size(max = 200, message = "O titulo deve ter no maximo 200 caracteres")
        String titulo,

        @NotBlank(message = "A descricao do chamado e obrigatoria")
        String corpo,

        @NotNull(message = "A categoria e obrigatoria")
        CategoriaChamado categoria,

        @NotNull(message = "A prioridade e obrigatoria")
        PrioridadeChamado prioridade,

        @Valid
        List<ChamadoAnexoRequest> anexos
) {
}
