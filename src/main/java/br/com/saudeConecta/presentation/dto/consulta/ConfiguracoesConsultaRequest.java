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

    @NotNull(message = "O campo pularParaConfirmado é obrigatório")
    private Boolean pularParaConfirmado;

    @NotBlank(message = "O campo descricao é obrigatório")
    private String descricao;
}
