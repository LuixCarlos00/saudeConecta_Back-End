package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO público (sem autenticação) — enviado pelo paciente ao assinar o planejamento terapêutico.
 */
@Getter
@Setter
@NoArgsConstructor
public class AssinaturaPlanjamentoRequest {

    @JsonProperty("token")
    private String token;

    @JsonProperty("assinaturaBase64")
    private String assinaturaBase64;
}
