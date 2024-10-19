package br.com.saudeConecta.endpoinst.consultaStatus.DTO;

import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

import java.util.List;
import java.util.stream.Collectors;

public record DadosConsultaStatusView(
        Long conSttCodigoConsulta,
        Medico conSttMedico,
        Paciente conSttPaciente,
        String conSttDia_semana,
        String conSttHorario,
        String conSttData,
        String conSttObservacao,
        String conSttDataCriacao,
        Byte conSttFormaPagamento,
        Byte conSttStatus,
        Long conSttAdm



) {
    public DadosConsultaStatusView(ConsultaStatus registro) {
        this(registro.getConSttCodigoConsulata(), registro.getConSttMedico(), registro.getConSttPaciente(), registro.getConSttDia_semana(), registro.getConSttHorario(),
                registro.getConSttData(), registro.getConSttObservacao() , registro.getConSttDataCriacao(), registro.getConSttFormaPagamento(),registro.getConSttStatus(),registro.getConSttAdm());
    }


    public static List<DadosConsultaStatusView> fromList(List<ConsultaStatus> registros) {
        return registros.stream()
                .map(DadosConsultaStatusView::new)
                .collect(Collectors.toList());
    }

}
