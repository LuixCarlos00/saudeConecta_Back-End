package br.com.saudeConecta.endpoinst.consulta.DTO;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;

public record DadosConsultaView(
        Long conCodigoConsulta,
        Medico conMedico,
        Paciente conPaciente,
        String conDia_semana,
        String conHorario,
        String conData,
        String conObservacoes,
        String conDataCriacao,
        Byte conFormaPagamento,
        String conStatus,
        Administrador conAdm

) {
    public DadosConsultaView(Consulta registro) {
        this(registro.getConCodigoConsulta(), registro.getConMedico(), registro.getConPaciente(), registro.getConDiaSemana(), registro.getConHorario(),
                registro.getConData(), registro.getConObservacoes() , registro.getConDataCriacao(), registro.getConFormaPagamento(),registro.getConStatus(),registro.getConAdm());
    }


    public DadosConsultaView(DadosConsultaView registro) {
        this(registro.conCodigoConsulta(), registro.conMedico(), registro.conPaciente(), registro.conDia_semana(), registro.conHorario(),
                registro.conData(), registro.conObservacoes() , registro.conDataCriacao(), registro.conFormaPagamento(),registro.conStatus(),registro.conAdm());
    }

}
