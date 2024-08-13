package br.com.saudeConecta.endpoinst.consultaStatus.DTO;

import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

import java.util.List;
import java.util.stream.Collectors;

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


    public static List<DadosConsultaStatusView> fromList(List<ConsultaStatus> registros) {
        return registros.stream()
                .map(DadosConsultaStatusView::new)
                .collect(Collectors.toList());
    }

}
