package br.com.saudeConecta.endpoinst.secretaria.Entity;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;

import java.util.List;


public class BuscarTodosUsuarios {
    private List<Paciente> paciente;
    private List<Medico> medico;
    private List<Secretaria> secretaria;
    private List<Administrador> administrador;


    public BuscarTodosUsuarios(List<Paciente> paciente, List<Medico> medico, List<Secretaria> secretaria, List<Administrador> administrador) {
        this.paciente = paciente;
        this.medico = medico;
        this.secretaria = secretaria;
        this.administrador = administrador;
    }
    public BuscarTodosUsuarios() {

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
