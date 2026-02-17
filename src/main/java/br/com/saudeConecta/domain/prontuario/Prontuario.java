package br.com.saudeConecta.domain.prontuario;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.sql.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prontuario")
@EqualsAndHashCode(of = "prontCodigoProntuario")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Prontuario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "prontCodigoProntuario", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prontCodigoProntuario;

    private String prontPeso;

    @Column(name = "prontAltura")
    private String prontAltura;

    @Column(name = "prontTemperatura")
    private String prontTemperatura;

    @Column(name = "prontDataNacimento", nullable = false)
    private String prontDataNacimento;

    @Column(name = "prontSexo")
    private String prontSexo;

    @Column(name = "prontSaturacao")
    private String prontSaturacao;

    @Column(name = "prontHemoglobina")
    private String prontHemoglobina;

    @Column(name = "prontPressao")
    private String prontPressao;

    @Column(name = "prontFrequenciaRespiratoria")
    private String prontFrequenciaRespiratoria;

    @Column(name = "prontFrequenciaArterialSistolica")
    private String prontFrequenciaArterialSistolica;

    @Column(name = "prontFrequenciaArterialDiastolica")
    private String prontFrequenciaArterialDiastolica;

    @Column(name = "prontObservacao")
    private String prontObservacao;

    @Column(name = "prontCondulta")
    private String prontCondulta;

    @Column(name = "prontAnamnese")
    private String prontAnamnese;

    @Column(name = "prontQueixaPricipal")
    private String prontQueixaPricipal;

    @Column(name = "prontDiagnostico")
    private String prontDiagnostico;

    @Column(name = "prontModeloPrescricao")
    private String prontModeloPrescricao;

    @Column(name = "prontTituloPrescricao")
    private String prontTituloPrescricao;

    @Column(name = "prontDataPrescricao")
    private String prontDataPrescricao;

    @Column(name = "prontPrescricao")
    private String prontPrescricao;

    @Column(name = "prontDataFinalizado", nullable = false)
    private Date prontDataFinalizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontCodigoMedico")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta")
    private Consulta consulta;

    @Column(name = "prontModeloExame")
    private String prontModeloExame;

    @Column(name = "prontTituloExame")
    private String prontTituloExame;

    @Column(name = "prontDataExame")
    private String prontDataExame;

    @Column(name = "prontExame")
    private String prontExame;

    @Column(name = "prontTempoDuracao")
    private String prontTempoDuracao;





    public Prontuario(@NotNull CadastrarProntuarioRequest dados, Profissional profissional, Consulta consulta) {
        this.prontAltura = dados.prontAltura();
        this.prontPeso = dados.prontPeso();
        this.prontTemperatura = dados.prontTemperatura();
        this.prontDataNacimento = dados.prontDataNacimento();
        this.prontSexo = dados.prontSexo();
        this.prontSaturacao = dados.prontSaturacao();
        this.prontHemoglobina = dados.prontHemoglobina();
        this.prontPressao = dados.prontPressao();
        this.prontFrequenciaRespiratoria = dados.prontFrequenciaRespiratoria();
        this.prontFrequenciaArterialSistolica = dados.prontFrequenciaArterialSistolica();
        this.prontFrequenciaArterialDiastolica = dados.prontFrequenciaArterialDiastolica();
        this.prontObservacao = dados.prontObservacao();
        this.prontCondulta = dados.prontCondulta();
        this.prontAnamnese = dados.prontAnamnese();
        this.prontQueixaPricipal = dados.prontQueixaPricipal();
        this.prontDiagnostico = dados.prontDiagnostico();
        this.prontModeloPrescricao = dados.prontModeloPrescricao();
        this.prontTituloPrescricao = dados.prontTituloPrescricao();
        this.prontDataPrescricao = dados.prontDataPrescricao();
        this.prontPrescricao = dados.prontPrescricao();
        this.prontDataFinalizado = dados.prontDataFinalizado();
        this.profissional = profissional;
        this.consulta = consulta;
        this.prontTituloExame = dados.prontTituloExame();
        this.prontDataExame = dados.prontDataExame();
        this.prontExame = dados.prontExame();
        this.prontModeloExame = dados.prontModeloExame();
        this.prontTempoDuracao = dados.prontTempoDuracao();
    }
}












