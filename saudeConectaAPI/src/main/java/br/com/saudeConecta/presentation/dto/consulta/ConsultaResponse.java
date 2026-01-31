package br.com.saudeConecta.presentation.dto.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;

public record ConsultaResponse(
        Long conCodigoConsulta,
        Long conMedicoId,
        String conMedicoNome,
        Long conPacienteId,
        String conPacienteNome,
        String conDiaSemana,
        String conHorario,
        String conData,
        String conObservacoes,
        String conDataCriacao,
        Byte conFormaPagamento,
        String conStatus,
        Long conAdmId
) {
    public ConsultaResponse(Consulta consulta) {
        this(
                consulta.getConCodigoConsulta(),
                consulta.getConMedico() != null ? consulta.getConMedico().getMedCodigo() : null,
                consulta.getConMedico() != null ? consulta.getConMedico().getMedNome() : null,
                consulta.getConPaciente() != null ? consulta.getConPaciente().getPaciCodigo() : null,
                consulta.getConPaciente() != null ? consulta.getConPaciente().getPaciNome() : null,
                consulta.getConDiaSemana(),
                consulta.getConHorario(),
                consulta.getConData(),
                consulta.getConObservacoes(),
                consulta.getConDataCriacao(),
                consulta.getConFormaPagamento(),
                consulta.getConStatus(),
                consulta.getConAdm() != null ? consulta.getConAdm().getAdmCodigo() : null
        );
    }
}
