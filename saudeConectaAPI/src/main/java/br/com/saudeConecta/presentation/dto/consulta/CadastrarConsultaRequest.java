package br.com.saudeConecta.presentation.dto.consulta;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastrarConsultaRequest(
        @NotNull(message = "ID do médico não pode ser nulo")
        @JsonProperty("ConMedico")
        Long conMedico,
        
        @NotNull(message = "ID do paciente não pode ser nulo")
        @JsonProperty("ConPaciente")
        Long conPaciente,
        
        @NotBlank(message = "Dia da semana não pode ser vazio")
        @Size(max = 20, message = "Dia da semana deve ter no máximo 20 caracteres")
        String conDia_semana,
        
        @NotBlank(message = "Horário não pode ser vazio")
        @Size(max = 10, message = "Horário deve ter no máximo 10 caracteres")
        String conHorario,
        
        @NotBlank(message = "Data não pode ser vazia")
        @Size(max = 20, message = "Data deve ter no máximo 20 caracteres")
        String conData,
        
        @Size(max = 500, message = "Observações deve ter no máximo 500 caracteres")
        String conObservacoes,
        
        @NotBlank(message = "Data de criação não pode ser vazia")
        @Size(max = 20, message = "Data de criação deve ter no máximo 20 caracteres")
        String conDadaCriacao,
        
        @NotNull(message = "Forma de pagamento não pode ser nula")
        Byte conFormaPagamento,
        
        @NotBlank(message = "Status não pode ser vazio")
        @Size(max = 20, message = "Status deve ter no máximo 20 caracteres")
        String conStatus,
        
        @NotNull(message = "ID do administrador não pode ser nulo")
        @JsonProperty("ConAdm")
        Long conAdm
) {
}
