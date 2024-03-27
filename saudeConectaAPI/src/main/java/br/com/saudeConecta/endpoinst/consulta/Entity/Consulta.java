package br.com.saudeConecta.endpoinst.consulta.Entity;

import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;


import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

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
    private Medico ConMedico;

    @ManyToOne
    @JoinColumn(name = "ConPaciente")
    private Paciente ConPaciente;


    @Column(name = "ConDia_semana ")
    private String ConDia_semana;

    @Column(name = "ConHorario")
    private Time ConHorario;

    @Column(name = "ConData")
    private Date ConData;

    @Column(name = "ConObservacoes")
    private String ConObservacoes;

//    public Consulta(Medico conMedico, Paciente conPaciente, String conDiaSemana,
//                    Time conHorario, Date conData, String conObservacoes) {
//        this.ConMedico = conMedico;
//        this.ConPaciente = conPaciente;
//        this.ConDia_semana = conDiaSemana;
//        this.ConHorario = conHorario;
//        this.ConData = conData;
//        this.ConObservacoes = conObservacoes;
//
//
//    }

    public Consulta(Medico medico, Paciente paciente, @NotNull DadosCadastraConsulta dados) {

        this.ConMedico = medico;
        this.ConPaciente = paciente;
        this.ConDia_semana = dados.ConDia_semana();
        this.ConHorario = dados.ConHorario();
        this.ConData = dados.ConData();
        this.ConObservacoes = dados.ConObservacoes();



    }
}
