package br.com.saudeConecta.endpoinst.consulta.DTO;


public record DadosCadastraConsulta(

        Long conMedico,
        Long conPaciente,
        String conDia_semana,
        String conHorario,
        String conData,
        String conObservacoes,
        String conDadaCriacao,
        Byte conFormaPagamento,
        Byte conStatus,
        Long conAdm

) {

}
