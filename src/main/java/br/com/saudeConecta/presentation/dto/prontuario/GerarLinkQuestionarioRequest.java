package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para solicitar a geração do link do questionário de saúde.
 */
@Getter
@Setter
@NoArgsConstructor
public class GerarLinkQuestionarioRequest {

    @JsonProperty("consultaId")
    private Long consultaId;
}
