package br.com.saudeConecta.endpoinst.prontuario.DTO;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;

import java.util.List;

/**
 * DTO que representa o histórico completo de um paciente,
 * contendo seus prontuários e consultas associadas.
 */
public class HistoricoPaciente {
    
    private final List<Prontuario> prontuarios;
    private final List<Consulta> consultas;

    public HistoricoPaciente(List<Prontuario> prontuarios, List<Consulta> consultas) {
        this.prontuarios = prontuarios;
        this.consultas = consultas;
    }

    public List<Prontuario> getProntuarios() {
        return prontuarios;
    }

    public List<Consulta> getConsultas() {
        return consultas;
    }
}