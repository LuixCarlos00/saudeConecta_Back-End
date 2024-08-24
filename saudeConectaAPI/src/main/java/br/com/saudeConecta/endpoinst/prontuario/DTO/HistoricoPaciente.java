package br.com.saudeConecta.endpoinst.prontuario.DTO;

import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import br.com.saudeConecta.endpoinst.prontuario.Entity.Prontuario;

import java.util.List;

public class HistoricoPaciente {
    private List<Prontuario> prontuarios;
    private List<ConsultaStatus> consultaStatus;

    public HistoricoPaciente(List<Prontuario> prontuarios, List<ConsultaStatus> consultaStatus) {
        this.prontuarios = prontuarios;
        this.consultaStatus = consultaStatus;
    }

    public List<Prontuario> getProntuarios() {
        return prontuarios;
    }

    public List<ConsultaStatus> getConsultaStatus() {
        return consultaStatus;
    }

}