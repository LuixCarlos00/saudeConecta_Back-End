package br.com.saudeConecta.domain.consulta;

import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.endpoinst.consulta.DTO.DadosCadastraConsulta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "consulta")
@EqualsAndHashCode(of = "conCodigoConsulta")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Consulta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConCodigoConsulta")
    private Long conCodigoConsulta;

    @ManyToOne
    @JoinColumn(name = "ConMedico")
    private Medico conMedico;

    @ManyToOne
    @JoinColumn(name = "ConPaciente")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "endereco"})
    private Paciente conPaciente;

    @Column(name = "ConDia_semana")
    private String conDiaSemana;

    @Column(name = "ConHorario")
    private String conHorario;

    @Column(name = "ConData")
    private String conData;

    @Column(name = "ConObservacoes")
    private String conObservacoes;

    @Column(name = "ConDataCriacao")
    private String conDataCriacao;

    @Column(name = "ConFormaPagamento")
    private Byte conFormaPagamento;

    @Column(name = "ConStatus")
    private String conStatus;

    @ManyToOne
    @JoinColumn(name = "ConAdm")
    @JsonIgnore
    private Administrador conAdm;

    public Consulta(Medico medico, Paciente paciente, Administrador adm, DadosCadastraConsulta dados) {
        this.conMedico = medico;
        this.conPaciente = paciente;
        this.conAdm = adm;
        this.conDiaSemana = dados.conDia_semana();
        this.conHorario = dados.conHorario();
        this.conData = dados.conData();
        this.conObservacoes = dados.conObservacoes();
        this.conDataCriacao = dados.conDadaCriacao();
        this.conFormaPagamento = dados.conFormaPagamento();
        this.conStatus = dados.conStatus();
    }

    public void update(Consulta dados) {
        this.conMedico = dados.getConMedico();
        this.conPaciente = dados.getConPaciente();
        this.conDiaSemana = dados.getConDiaSemana();
        this.conHorario = dados.getConHorario();
        this.conData = dados.getConData();
        this.conObservacoes = dados.getConObservacoes();
        this.conDataCriacao = dados.getConDataCriacao();
        this.conFormaPagamento = dados.getConFormaPagamento();
        this.conStatus = dados.getConStatus();
        this.conAdm = dados.getConAdm();
    }
}
