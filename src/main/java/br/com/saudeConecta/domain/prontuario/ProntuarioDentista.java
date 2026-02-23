package br.com.saudeConecta.domain.prontuario;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prontuario_dentista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer"})   // igual ao Prontuario médico
public class ProntuarioDentista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prontdent_codigo")
    private Long codigo;

    @Column(name = "prontdent_queixa_principal", columnDefinition = "TEXT")
    private String queixaPrincipal;

    @Column(name = "prontdent_anamnese", columnDefinition = "TEXT")
    private String anamnese;

    @Column(name = "prontdent_observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "prontdent_higiene_bucal", length = 50)
    private String higieneBucal;

    @Column(name = "prontdent_condicao_gengival", length = 100)
    private String condicaoGengival;

    @Column(name = "prontdent_oclusal", columnDefinition = "TEXT")
    private String oclusal;

    @Column(name = "prontdent_atm", columnDefinition = "TEXT")
    private String atm;

    @Column(name = "prontdent_diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "prontdent_plano_tratamento", columnDefinition = "TEXT")
    private String planoTratamento;

    @Column(name = "prontdent_titulo_prescricao", length = 255)
    private String tituloPrescricao;

    @Column(name = "prontdent_data_prescricao", length = 50)
    private String dataPrescricao;

    @Column(name = "prontdent_prescricao", columnDefinition = "TEXT")
    private String prescricao;

    @Column(name = "prontdent_titulo_exame", length = 255)
    private String tituloExame;

    @Column(name = "prontdent_data_exame", length = 50)
    private String dataExame;

    @Column(name = "prontdent_procedimentos", columnDefinition = "TEXT")
    private String procedimentos;

    @Column(name = "prontdent_orientacoes", columnDefinition = "TEXT")
    private String orientacoes;

    @Column(name = "prontdent_data_finalizado", nullable = false)
    private LocalDate dataFinalizado;

    @Column(name = "prontdent_tempo_duracao", length = 50)
    private String tempoDuracao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontdent_codigo_medico", referencedColumnName = "id")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontdent_consulta", referencedColumnName = "id")
    private Consulta consulta;

    // ── Quebra o ciclo: ProntuarioDentistaDente → prontuarioDentista → dentes → ... ──
    @OneToMany(
            mappedBy = "prontuarioDentista",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("prontuarioDentista")
    @Builder.Default
    private List<ProntuarioDentistaDente> dentes = new ArrayList<>();

    public void addDente(ProntuarioDentistaDente dente) {
        dente.setProntuarioDentista(this);
        this.dentes.add(dente);
    }
}