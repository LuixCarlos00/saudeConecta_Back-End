package br.com.saudeConecta.endpoinst.consultaStatus.DTO;


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


