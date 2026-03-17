package br.com.saudeConecta.presentation.dto.prontuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO wrapper para cadastrar prontuário médico com planejamentos terapêuticos
 */
@Getter
@Setter
@NoArgsConstructor
public class CadastrarProntuarioComPlanejamentosRequest {

    @Valid
    @NotNull(message = "Dados do prontuário são obrigatórios")
    private CadastrarProntuarioRequest prontuario;

    private List<PlanejamentoTerapeuticoRequest> planejamentos;
}
