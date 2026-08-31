package br.com.saudeConecta.presentation.dto.consulta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracoesConsultaRequest {

    @NotNull(message = "O campo pularParaAgendado é obrigatório")
    private Boolean pularParaAgendado;

    @NotBlank(message = "O campo descricao é obrigatório")
    private String descricao;
}
