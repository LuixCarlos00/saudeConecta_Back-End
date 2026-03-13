package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para cadastro de procedimento padrão pelo profissional.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProcedimentoPadraoRequest {

    @JsonProperty("nomeProcedimento")
    private String nomeProcedimento;

    @JsonProperty("valorPadrao")
    private BigDecimal valorPadrao;
}
