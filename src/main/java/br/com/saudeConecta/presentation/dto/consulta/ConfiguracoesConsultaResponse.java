package br.com.saudeConecta.presentation.dto.consulta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracoesConsultaResponse {

    private Long id;
    private Long organizacaoId;
    private Boolean pularParaConfirmado;
    private String descricao;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
