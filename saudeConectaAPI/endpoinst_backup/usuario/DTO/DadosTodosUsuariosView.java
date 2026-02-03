package br.com.saudeConecta.endpoinst.usuario.DTO;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.secretaria.Secretaria;

import java.util.List;


public class DadosTodosUsuariosView {
    private List<Paciente> paciente;
    private List<Medico> medico;
    private List<Secretaria> secretaria;
    private List<Administrador> administrador;


    public DadosTodosUsuariosView(List<Paciente> paciente, List<Medico> medico, List<Secretaria> secretaria, List<Administrador> administrador) {
        this.paciente = paciente;
        this.medico = medico;
        this.secretaria = secretaria;
        this.administrador = administrador;
    }
    public DadosTodosUsuariosView() {

    }

    public List<Paciente> getPaciente() {
        return paciente;
    }

    public void setPaciente(List<Paciente> paciente) {
        this.paciente = paciente;
    }

    public List<Medico> getMedico() {
        return medico;
    }

    public void setMedico(List<Medico> medico) {
        this.medico = medico;
    }

    public List<Secretaria> getSecretaria() {
        return secretaria;
    }

    public void setSecretaria(List<Secretaria> secretaria) {
        this.secretaria = secretaria;
    }

    public List<Administrador> getAdministrador() {
        return administrador;
    }

    public void setAdministrador(List<Administrador> administrador) {
        this.administrador = administrador;
    }
}
