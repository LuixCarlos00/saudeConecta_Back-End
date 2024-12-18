package br.com.saudeConecta.endpoinst.consulta.Entity;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;


import java.io.Serializable;
import java.util.Date;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "consulta")

public class Consulta implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConCodigoConsulta")
    private Long ConCodigoConsulta;

    @ManyToOne
    @JoinColumn(name = "ConMedico")
    private Medico conMedico;

    @ManyToOne
    @JoinColumn(name = "ConPaciente")
    private Paciente conPaciente;
    
    @Column(name = "ConDia_semana ")
    private String ConDia_semana;

    @Column(name = "ConHorario")
    private String conHorario;

    @Column(name = "ConData")
    private String conData;

    @Column(name = "ConObservacoes")
    private String ConObservacoes;

    @Column(name = "ConDataCriacao")
    private String ConDataCriacao;

    @Column(name = "ConFormaPagamento")
    private Byte ConFormaPagamento;

    @Column(name = "ConStatus")
    private Byte ConStatus;

    @ManyToOne
    @JoinColumn(name = "ConAdm")
    private Administrador ConAdm;
    
 
    public Consulta(Medico medico, Paciente paciente,Administrador administrador  , @NotNull DadosCadastraConsulta dados) {

        this.conMedico = medico;
        this.conPaciente = paciente;
        this.ConDia_semana = dados.conDia_semana();
        this.conHorario = dados.conHorario();
        this.conData = dados.conData();
        this.ConObservacoes = dados.conObservacoes();
        this.ConDataCriacao = dados.conDadaCriacao();
        this.ConFormaPagamento = dados.conFormaPagamento();
        this.ConAdm =  administrador;
        this.ConStatus = dados.conStatus();


    }

    public void update(Consulta consulta) {

        this.ConObservacoes = consulta.ConObservacoes;
        this.ConDataCriacao = consulta.ConDataCriacao;
        this.ConFormaPagamento = consulta.ConFormaPagamento;
        this.ConStatus = consulta.ConStatus;
        this.ConAdm = consulta.ConAdm;
        this.conMedico = consulta.conMedico;
        this.conPaciente = consulta.conPaciente;
        this.ConDia_semana = consulta.ConDia_semana;
        this.conHorario = consulta.conHorario;
        this.conData = consulta.conData;
    }


}
