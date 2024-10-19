package br.com.saudeConecta.endpoinst.consulta.DTO;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

import java.util.Date;

public record DadosConsultaView(
        Long conCodigoConsulta,
        Medico conMedico,
        Paciente conPaciente,
        String conDia_semana,
        String conHorario,
        String conData,
        String conObservacoes,
        String conDadaCriacao,
        Byte conFormaPagamento,
        Byte conStatus,
        Administrador conAdm

) {
    public DadosConsultaView(Consulta registro) {
        this(registro.getConCodigoConsulta(), registro.getConMedico(), registro.getConPaciente(), registro.getConDia_semana(), registro.getConHorario(),
                registro.getConData(), registro.getConObservacoes() , registro.getConDataCriacao(), registro.getConFormaPagamento(),registro.getConStatus(),registro.getConAdm());
    }


    public DadosConsultaView(DadosConsultaView registro) {
        this(registro.conCodigoConsulta(), registro.conMedico(), registro.conPaciente(), registro.conDia_semana(), registro.conHorario(),
                registro.conData(), registro.conObservacoes() , registro.conDadaCriacao(), registro.conFormaPagamento(),registro.conStatus(),registro.conAdm());
    }

}
