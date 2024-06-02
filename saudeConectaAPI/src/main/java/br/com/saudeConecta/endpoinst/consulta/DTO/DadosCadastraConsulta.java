package br.com.saudeConecta.endpoinst.consulta.DTO;


public record DadosCadastraConsulta(

        Long ConMedico,
        Long ConPaciente,
        String ConDia_semana,
        String ConHorario,
        String ConData,
        String ConObservacoes,
        String ConDadaCriacao,
        Byte ConFormaPagamento,
        Byte ConStatus,
        Long ConAdm

) {

}
