package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CadastrarProntuarioDentistaRequest {
    // ── Anamnese ─────────────────────────────────────────────────────────────

    @JsonProperty("queixaPrincipal")
    private String queixaPrincipal;

    @JsonProperty("anamnese")
    private String anamnese;

    @JsonProperty("observacao")
    private String observacao;

    // ── Exame Clínico — campos estruturados ───────────────────────────────────

    @JsonProperty("higieneBucal")
    private String higieneBucal;

    @JsonProperty("condicaoGengival")
    private String condicaoGengival;

    @JsonProperty("oclusal")
    private String oclusal;

    @JsonProperty("atm")
    private String atm;

    // ── Odontograma — lista de dentes ─────────────────────────────────────────

    /**
     * Lista completa dos 32 dentes. Exemplo de item:
     * { "numeroFdi": 22, "status": "cariado", "observacao": "dente quebrado" }
     */
    @JsonProperty("odontograma")
    private List<DenteRequest> odontograma;

    // ── Diagnóstico e Tratamento ──────────────────────────────────────────────

    @JsonProperty("diagnostico")
    private String diagnostico;

    @JsonProperty("planoTratamento")
    private String planoTratamento;

    // ── Prescrição ────────────────────────────────────────────────────────────

    @JsonProperty("prescricao")
    private String prescricao;

    @JsonProperty("tituloPrescricao")
    private String tituloPrescricao;

    @JsonProperty("dataPrescricao")
    private String dataPrescricao;

    // ── Procedimentos / Exame ─────────────────────────────────────────────────

    @JsonProperty("procedimentos")
    private String procedimentos;

    @JsonProperty("orientacoes")
    private String orientacoes;

    @JsonProperty("tituloExame")
    private String tituloExame;

    @JsonProperty("dataExame")
    private String dataExame;

    // ── Controle ──────────────────────────────────────────────────────────────

    @JsonProperty("dataFinalizado")
    private String dataFinalizado;

    @JsonProperty("tempoDuracao")
    private String tempoDuracao;

    // ── Identificação do Paciente (endereço vem da entidade Paciente) ──────

    @JsonProperty("responsavel")
    private String responsavel;

    @JsonProperty("inicioTratamento")
    private String inicioTratamento;

    @JsonProperty("terminoTratamento")
    private String terminoTratamento;

    @JsonProperty("interrupcao")
    private String interrupcao;

    // ── Exame Objetivo — Sinais Vitais ───────────────────────────────────────

    @JsonProperty("pressaoArterial")
    private String pressaoArterial;

    @JsonProperty("pulso")
    private String pulso;

    @JsonProperty("altura")
    private String altura;

    @JsonProperty("temperatura")
    private String temperatura;

    @JsonProperty("peso")
    private String peso;

    @JsonProperty("edema")
    private String edema;

    @JsonProperty("facies")
    private String facies;

    @JsonProperty("linfonodos")
    private String linfonodos;

    @JsonProperty("labios")
    private String labios;

    @JsonProperty("mucosas")
    private String mucosas;

    @JsonProperty("soalhoBucal")
    private String soalhoBucal;

    @JsonProperty("palato")
    private String palato;

    @JsonProperty("orofaringe")
    private String orofaringe;

    // ── Exame Objetivo — Exame Intrabucal ────────────────────────────────────

    @JsonProperty("lingua")
    private String lingua;

    @JsonProperty("gengiva")
    private String gengiva;

    @JsonProperty("habitosNocivos")
    private String habitosNocivos;

    @JsonProperty("portadorAparelho")
    private String portadorAparelho;

    @JsonProperty("exameOutros")
    private String exameOutros;

    // ── TUSS e CID ──────────────────────────────────────────────────────────

    @JsonProperty("tussTexto")
    private String tussTexto;

    @JsonProperty("cidTexto")
    private String cidTexto;

    @JsonProperty("solicitacaoExameTexto")
    private String solicitacaoExameTexto;

    // ── Relacionamentos ───────────────────────────────────────────────────────

    @JsonProperty("codigoMedico")
    private Long codigoMedico;

    @JsonProperty("consulta")
    private Long consulta;

    // ── Planejamento Terapêutico ────────────────────────────────────────────

    @JsonProperty("planejamentos")
    private List<PlanejamentoItem> planejamentos;

    // ── DTO interno para cada dente ───────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DenteRequest {

        /** Número FDI: 11–18, 21–28, 31–38, 41–48 */
        @JsonProperty("numeroFdi")
        private Integer numeroFdi;

        /** sadio | cariado | obturado | ausente | protese | canal | fratura | implante */
        @JsonProperty("status")
        private String status;

        /** Observação livre do dentista sobre este dente */
        @JsonProperty("observacao")
        private String observacao;
    }

    // ── DTO interno para cada item de planejamento terapêutico ──────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PlanejamentoItem {

        @JsonProperty("dataProcedimento")
        private String dataProcedimento;

        @JsonProperty("procedimentoRealizado")
        private String procedimentoRealizado;

        @JsonProperty("valor")
        private BigDecimal valor;

        @JsonProperty("pacienteId")
        private Long pacienteId;
    }
}