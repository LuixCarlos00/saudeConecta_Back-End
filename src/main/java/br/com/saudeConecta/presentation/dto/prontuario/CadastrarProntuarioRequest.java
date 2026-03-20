package br.com.saudeConecta.presentation.dto.prontuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CadastrarProntuarioRequest {
    // ── Sinais Vitais ─────────────────────────────────────────────────────
    @JsonProperty("altura")
    private String altura;

    @JsonProperty("peso")
    private String peso;

    @JsonProperty("temperatura")
    private String temperatura;

    @JsonProperty("saturacao")
    private String saturacao;

    @JsonProperty("hemoglobina")
    private String hemoglobina;

    @JsonProperty("pressao")
    private String pressao;

    @JsonProperty("frequenciaRespiratoria")
    private String frequenciaRespiratoria;

    @JsonProperty("frequenciaArterialSistolica")
    private String frequenciaArterialSistolica;

    @JsonProperty("frequenciaArterialDiastolica")
    private String frequenciaArterialDiastolica;

    @JsonProperty("pulso")
    private String pulso;

    // ── Anamnese e Avaliação ──────────────────────────────────────────────
    @JsonProperty("observacao")
    private String observacao;

    @JsonProperty("anamnese")
    private String anamnese;

    @JsonProperty("queixaPrincipal")
    private String queixaPrincipal;

    @JsonProperty("diagnostico")
    private String diagnostico;

    // ── Prescrição ────────────────────────────────────────────────────────
    @JsonProperty("modeloPrescricao")
    private String modeloPrescricao;

    @JsonProperty("tituloPrescricao")
    private String tituloPrescricao;

    @JsonProperty("dataPrescricao")
    private String dataPrescricao;

    @JsonProperty("prescricao")
    private String prescricao;

    // ── Controle ──────────────────────────────────────────────────────────
    @JsonProperty("dataFinalizado")
    @NotNull(message = "Data finalizado não pode ser nula")
    private Date dataFinalizado;

    @JsonProperty("tempoDuracao")
    private String tempoDuracao;

    // ── Relacionamentos ───────────────────────────────────────────────────
    @JsonProperty("codigoMedico")
    @NotNull(message = "ID do médico não pode ser nulo")
    private Long codigoMedico;

    @JsonProperty("consulta")
    @NotNull(message = "ID da consulta não pode ser nulo")
    private Long consulta;

    // ── Identificação do Paciente ─────────────────────────────────────────
    @JsonProperty("responsavel")
    private String responsavel;

    @JsonProperty("exameOutros")
    private String exameOutros;

    // ── Exames ────────────────────────────────────────────────────────────
    @JsonProperty("modeloExame")
    private String modeloExame;

    @JsonProperty("tituloExame")
    private String tituloExame;

    @JsonProperty("dataExame")
    private String dataExame;

    @JsonProperty("exame")
    private String exame;

    // ── Tratamento ───────────────────────────────────────────────────────
    @JsonProperty("inicioTratamento")
    private Date inicioTratamento;

    @JsonProperty("terminoTratamento")
    private Date terminoTratamento;

    @JsonProperty("interrupcao")
    private String interrupcao;

    // ── Diagnóstico e Tratamento ──────────────────────────────────────────
    @JsonProperty("orientacoes")
    private String orientacoes;

    // ── TUSS e CID ────────────────────────────────────────────────────────
    @JsonProperty("tussTexto")
    private String tussTexto;

    @JsonProperty("cidTexto")
    private String cidTexto;

    @JsonProperty("solicitacaoExameTexto")
    private String solicitacaoExameTexto;

    // ── Planejamento Terapêutico ────────────────────────────────────────────
    @JsonProperty("planejamentos")
    private List<PlanejamentoItem> planejamentos;

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
