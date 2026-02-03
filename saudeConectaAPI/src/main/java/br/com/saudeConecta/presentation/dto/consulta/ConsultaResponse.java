package br.com.saudeConecta.presentation.dto.consulta;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;

public record ConsultaResponse(
        Long conCodigoConsulta,
        String conDiaSemana,
        String conHorario,
        String conData,
        String conObservacoes,
        String conDataCriacao,
        Byte conFormaPagamento,
        String conStatus,
        Medico medico,
        Paciente paciente,
        Administrador adm
) {
    public ConsultaResponse(Consulta consulta) {
        this(
                consulta.getConCodigoConsulta(),
                consulta.getConDiaSemana(),
                consulta.getConHorario(),
                consulta.getConData(),
                consulta.getConObservacoes(),
                consulta.getConDataCriacao(),
                consulta.getConFormaPagamento(),
                consulta.getConStatus(),
                consulta.getConMedico(),
                consulta.getConPaciente(),
                consulta.getConAdm()
        );
    }
}
