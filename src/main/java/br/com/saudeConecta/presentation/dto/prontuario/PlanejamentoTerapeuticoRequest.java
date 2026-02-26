package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para cadastro de item do planejamento terapêutico.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlanejamentoTerapeuticoRequest {

    @JsonProperty("prontuarioDentistaId")
    private Long prontuarioDentistaId;

    @JsonProperty("consultaId")
    private Long consultaId;

    @JsonProperty("pacienteId")
    private Long pacienteId;

    @JsonProperty("dataProcedimento")
    private String dataProcedimento;

    @JsonProperty("procedimentoRealizado")
    private String procedimentoRealizado;

    @JsonProperty("valor")
    private BigDecimal valor;
}
