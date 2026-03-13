package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO público (sem autenticação) — enviado pelo paciente ao responder o questionário de saúde.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionarioSaudeRequest {

    @JsonProperty("token")
    private String token;

    @JsonProperty("respostasQuestionario")
    private String respostasQuestionario;

    @JsonProperty("assinaturaBase64")
    private String assinaturaBase64;
}
