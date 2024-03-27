package br.com.saudeConecta.endpoinst.consulta.DTO;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

import java.sql.Date;
import java.sql.Time;

public record DadosConsultaView(
        Long ConCodigoConsulta,
        Medico ConMedico,
        Paciente ConPaciente,
        String ConDia_semana,
        Time ConHorario,
        Date ConData,
        String ConObservacoes

) {
    public DadosConsultaView(Consulta registro) {
        this(registro.getConCodigoConsulta(), registro.getConMedico(), registro.getConPaciente(), registro.getConDia_semana(), registro.getConHorario(),
                registro.getConData(), registro.getConObservacoes() );
    }



}
