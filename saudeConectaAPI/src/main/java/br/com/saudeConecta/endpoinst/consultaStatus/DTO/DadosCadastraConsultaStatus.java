package br.com.saudeConecta.endpoinst.consultaStatus.DTO;


import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public record DadosCadastraConsultaStatus(

        Long ConSttMedico,
        Long ConSttPaciente,
        String ConSttDia_semana,
        String ConSttHorario,
        String ConSttData,
        String ConSttObservacao,
        String ConSttDataCriacao,
        Byte ConSttFormaPagamento,
        Byte ConSttStatus,
        Long ConSttAdm

) {

}


