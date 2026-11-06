package br.com.saudeConecta.domain.prontuario;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

//Prontuario Médico
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "prontTituloPrescricao")
    private String prontTituloPrescricao;

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

    @OneToMany(
            mappedBy = "prontuario",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("prontuario")
    @Builder.Default
    private List<PlanejamentoTerapeutico> planejamentos = new ArrayList<>();

    public void addPlanejamento(PlanejamentoTerapeutico planejamento) {
        planejamento.setProntuario(this);
        this.planejamentos.add(planejamento);
    }
}
