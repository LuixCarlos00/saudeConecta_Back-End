package br.com.saudeConecta.domain.prontuario;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
//Prntuario de dentista
@Entity
@Table(name = "prontuario_dentista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer"})
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

    @Column(name = "prontdent_higiene_bucal", length = 250)
    private String higieneBucal;

    @Column(name = "prontdent_condicao_gengival", length = 250)
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


    @Column(name = "prontdent_responsavel", length = 255)
    private String responsavel;

    @Column(name = "prontdent_inicio_tratamento")
    private LocalDate inicioTratamento;

    @Column(name = "prontdent_termino_tratamento")
    private LocalDate terminoTratamento;

    @Column(name = "prontdent_interrupcao", length = 500)
    private String interrupcao;

    // ── Exame Objetivo — Sinais Vitais ──────────────────────────────────────
    @Column(name = "prontdent_pressao_arterial", length = 250)
    private String pressaoArterial;

    @Column(name = "prontdent_pulso", length = 250)
    private String pulso;

    @Column(name = "prontdent_altura", length = 250)
    private String altura;

    @Column(name = "prontdent_temperatura", length = 250)
    private String temperatura;

    @Column(name = "prontdent_peso", length = 250)
    private String peso;

    @Column(name = "prontdent_edema", length = 500)
    private String edema;

    @Column(name = "prontdent_facies", length = 500)
    private String facies;

    @Column(name = "prontdent_linfonodos", length = 500)
    private String linfonodos;

    @Column(name = "prontdent_labios", length = 500)
    private String labios;

    @Column(name = "prontdent_mucosas", length = 500)
    private String mucosas;

    @Column(name = "prontdent_soalho_bucal", length = 500)
    private String soalhoBucal;

    @Column(name = "prontdent_palato", length = 500)
    private String palato;

    @Column(name = "prontdent_orofaringe", length = 500)
    private String orofaringe;

    // ── Exame Objetivo — Exame Intrabucal ───────────────────────────────────
    @Column(name = "prontdent_lingua", length = 500)
    private String lingua;

    @Column(name = "prontdent_gengiva", length = 500)
    private String gengiva;

    @Column(name = "prontdent_habitos_nocivos", length = 500)
    private String habitosNocivos;

    @Column(name = "prontdent_portador_aparelho", length = 250)
    private String portadorAparelho;

    @Column(name = "prontdent_exame_outros", columnDefinition = "TEXT")
    private String exameOutros;

    // ── TUSS e CID ─────────────────────────────────────────────────────────
    @Column(name = "prontdent_tuss_texto", columnDefinition = "TEXT")
    private String tussTexto;

    @Column(name = "prontdent_cid_texto", columnDefinition = "TEXT")
    private String cidTexto;

    @Column(name = "prontdent_solicitacao_exame_texto", columnDefinition = "TEXT")
    private String solicitacaoExameTexto;

    // ── Relacionamentos ─────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontdent_codigo_medico", referencedColumnName = "id")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontdent_consulta", referencedColumnName = "id")
    private Consulta consulta;

    @OneToMany(
            mappedBy = "prontuarioDentista",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("prontuarioDentista")
    @Builder.Default
    private Set<ProntuarioDentistaDente> dentes = new HashSet<>();

    @OneToMany(
            mappedBy = "prontuarioDentista",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("prontuarioDentista")
    @Builder.Default
    private List<PlanejamentoTerapeutico> planejamentos = new ArrayList<>();

    public void addDente(ProntuarioDentistaDente dente) {
        dente.setProntuarioDentista(this);
        this.dentes.add(dente);
    }

    public void addPlanejamento(PlanejamentoTerapeutico planejamento) {
        planejamento.setProntuarioDentista(this);
        this.planejamentos.add(planejamento);
    }
}