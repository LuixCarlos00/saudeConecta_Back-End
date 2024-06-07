package br.com.saudeConecta.endpoinst.consultaStatus.DTO;

import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

public record DadosConsultaStatusView(
        Long ConSttCodigoConsulata,
        Medico ConSttMedico,
        Paciente ConSttPaciente,
        String ConSttDia_semana,
        String ConSttHorario,
        String ConSttData,
        String ConSttObservacao,
        String ConSttDataCriacao,
        Byte ConSttFormaPagamento,
        Byte ConSttStatus,
        Long ConSttAdm



) {
    public DadosConsultaStatusView(ConsultaStatus registro) {
        this(registro.getConSttCodigoConsulata(), registro.getConSttMedico(), registro.getConSttPaciente(), registro.getConSttDia_semana(), registro.getConSttHorario(),
                registro.getConSttData(), registro.getConSttObservacao() , registro.getConSttDataCriacao(), registro.getConSttFormaPagamento(),registro.getConSttStatus(),registro.getConSttAdm());
    }


    public DadosConsultaStatusView(DadosConsultaStatusView registro) {
        this(registro.ConSttCodigoConsulata(), registro.ConSttMedico(), registro.ConSttPaciente(), registro.ConSttDia_semana(), registro.ConSttHorario(),
                registro.ConSttData(), registro.ConSttObservacao() , registro.ConSttDataCriacao(), registro.ConSttFormaPagamento(),registro.ConSttStatus(),registro.ConSttAdm());
    }

}
