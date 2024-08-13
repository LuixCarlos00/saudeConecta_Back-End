package br.com.saudeConecta.endpoinst.consultaStatus.Entity;

import br.com.saudeConecta.endpoinst.consultaStatus.DTO.DadosCadastraConsultaStatus;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "consultastatus")

public class ConsultaStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConSttCodigoConsulata")
    private Long conSttCodigoConsulata;

    @ManyToOne
    @JoinColumn(name = "ConSttMedico")
    private Medico conSttMedico;

    @ManyToOne
    @JoinColumn(name = "ConSttPaciente")
    private Paciente conSttPaciente;
    
    @Column(name = "ConSttDia_semana ")
    private String ConSttDia_semana;

    @Column(name = "ConSttHorario")
    private String conSttHorario;

    @Column(name = "ConSttData")
    private String conSttData;

    @Column(name = "ConSttObservacao")
    private String ConSttObservacao;

    @Column(name = "ConSttDataCriacao")
    private String conSttDataCriacao;

    @Column(name = "ConSttFormaPagamento")
    private Byte ConSttFormaPagamento;

    @Column(name = "ConSttStatus")
    private Byte ConSttStatus;

    @Column(name = "ConSttAdm")
    private Long conSttAdm  ;








    public ConsultaStatus(Medico medico, Paciente paciente, @NotNull DadosCadastraConsultaStatus dados) {

        this.conSttMedico = medico;
        this.conSttPaciente = paciente;
        this.ConSttDia_semana =dados.ConSttDia_semana();
        this.conSttHorario = dados.ConSttHorario();
        this.conSttData = dados.ConSttData();
        this.ConSttObservacao = dados.ConSttObservacao();
        this.conSttDataCriacao = dados.ConSttDataCriacao();
        this.ConSttFormaPagamento = dados.ConSttFormaPagamento();
        this.conSttAdm = dados.ConSttAdm();
        this.ConSttStatus = dados.ConSttStatus();


    }

    public void update(ConsultaStatus consulta) {

        this.ConSttObservacao = consulta.ConSttObservacao;
        this.conSttDataCriacao = consulta.conSttDataCriacao;
        this.ConSttFormaPagamento = consulta.ConSttFormaPagamento;
        this.ConSttStatus = consulta.ConSttStatus;
        this.conSttAdm = consulta.conSttAdm;
        this.conSttMedico = consulta.conSttMedico;
        this.conSttPaciente = consulta.conSttPaciente;
        this.ConSttDia_semana = consulta.ConSttDia_semana;
        this.conSttHorario = consulta.conSttHorario;
        this.conSttData = consulta.conSttData;
    }


}
