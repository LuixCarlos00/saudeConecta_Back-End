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
import java.time.LocalDate;

//Prontuario Médico
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

    @Column(name = "prontObservacao", columnDefinition = "TEXT")
    private String prontObservacao;


    @Column(name = "prontAnamnese", columnDefinition = "TEXT")
    private String prontAnamnese;

    @Column(name = "prontQueixaPricipal", columnDefinition = "TEXT")
    private String prontQueixaPricipal;

    @Column(name = "prontDiagnostico", columnDefinition = "TEXT")
    private String prontDiagnostico;

    @Column(name = "prontModeloPrescricao")
    private String prontModeloPrescricao;

    @Column(name = "prontTituloPrescricao")
    private String prontTituloPrescricao;

    @Column(name = "prontDataPrescricao")
    private String prontDataPrescricao;

    @Column(name = "prontPrescricao", columnDefinition = "TEXT")
    private String prontPrescricao;

    @Column(name = "prontDataFinalizado", nullable = false)
    private Date prontDataFinalizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontCodigoMedico")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta")
    private Consulta consulta;


    @Column(name = "prontTempoDuracao")
    private String prontTempoDuracao;

    // ── Identificação do Paciente ─────────────────────────────────────────────
    @Column(name = "prontResponsavel", length = 255)
    private String prontResponsavel;

    // ── Sinais Vitais (campo faltante) ────────────────────────────────────────
    @Column(name = "prontPulso", length = 250)
    private String prontPulso;



    @Column(name = "prontExameOutros", columnDefinition = "TEXT")
    private String prontExameOutros;

    // ── Diagnóstico e Tratamento ──────────────────────────────────────────────
    @Column(name = "prontOrientacoes", columnDefinition = "TEXT")
    private String prontOrientacoes;

    // ── TUSS e CID ────────────────────────────────────────────────────────────
    @Column(name = "prontTussTexto", columnDefinition = "TEXT")
    private String prontTussTexto;

    @Column(name = "prontCidTexto", columnDefinition = "TEXT")
    private String prontCidTexto;

    @Column(name = "prontSolicitacaoExameTexto", columnDefinition = "TEXT")
    private String prontSolicitacaoExameTexto;

    public Prontuario(@NotNull CadastrarProntuarioRequest dados, Profissional profissional, Consulta consulta) {
        this.prontAltura = dados.altura();
        this.prontPeso = dados.peso();
        this.prontTemperatura = dados.temperatura();
        this.prontSaturacao = dados.saturacao();
        this.prontHemoglobina = dados.hemoglobina();
        this.prontPressao = dados.pressao();
        this.prontFrequenciaRespiratoria = dados.frequenciaRespiratoria();
        this.prontFrequenciaArterialSistolica = dados.frequenciaArterialSistolica();
        this.prontFrequenciaArterialDiastolica = dados.frequenciaArterialDiastolica();
        this.prontObservacao = dados.observacao();
        this.prontAnamnese = dados.anamnese();
        this.prontQueixaPricipal = dados.queixaPrincipal();
        this.prontDiagnostico = dados.diagnostico();
        this.prontModeloPrescricao = dados.modeloPrescricao();
        this.prontTituloPrescricao = dados.tituloPrescricao();
        this.prontDataPrescricao = dados.dataPrescricao();
        this.prontPrescricao = dados.prescricao();
        this.prontDataFinalizado = dados.dataFinalizado();
        this.profissional = profissional;
        this.consulta = consulta;
        this.prontTempoDuracao = dados.tempoDuracao();
        // Novos campos
        this.prontResponsavel = dados.responsavel();
        this.prontPulso = dados.pulso();
        this.prontExameOutros = dados.exameOutros();
        this.prontOrientacoes = dados.orientacoes();
        this.prontTussTexto = dados.tussTexto();
        this.prontCidTexto = dados.cidTexto();
        this.prontSolicitacaoExameTexto = dados.solicitacaoExameTexto();
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            return LocalDate.parse(data);
        } catch (Exception e) {
            return null;
        }
    }
}












