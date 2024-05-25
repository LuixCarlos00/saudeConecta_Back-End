package br.com.saudeConecta.endpoinst.consulta.DTO;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

import java.util.Date;

public record DadosConsultaView(
        Long ConCodigoConsulta,
        Medico ConMedico,
        Paciente ConPaciente,
        String ConDia_semana,
        String ConHorario,
        String ConData,
        String ConObservacoes,
        String ConDadaCriacao,
        Byte ConFormaPagamento,
        Long ConAdm

) {
    public DadosConsultaView(Consulta registro) {
        this(registro.getConCodigoConsulta(), registro.getConMedico(), registro.getConPaciente(), registro.getConDia_semana(), registro.getConHorario(),
                registro.getConData(), registro.getConObservacoes() , registro.getConDataCriacao(), registro.getConFormaPagamento(),registro.getConAdm());
    }



}
