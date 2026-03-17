package br.com.saudeConecta.presentation.dto.prontuario;

import jakarta.validation.constraints.NotNull;

import java.sql.Date;

public record CadastrarProntuarioRequest(
        // ── Sinais Vitais ─────────────────────────────────────────────────────
        String altura,
        String peso,
        String temperatura,
        String saturacao,
        String hemoglobina,
        String pressao,
        String frequenciaRespiratoria,
        String frequenciaArterialSistolica,
        String frequenciaArterialDiastolica,
        String pulso,

        // ── Anamnese e Avaliação ──────────────────────────────────────────────
        String observacao,
        String anamnese,
        String queixaPrincipal,
        String diagnostico,

        // ── Prescrição ────────────────────────────────────────────────────────
        String modeloPrescricao,
        String tituloPrescricao,
        String dataPrescricao,
        String prescricao,

        // ── Controle ──────────────────────────────────────────────────────────
        @NotNull(message = "Data finalizado não pode ser nula")
        Date dataFinalizado,
        String tempoDuracao,


        // ── Relacionamentos ───────────────────────────────────────────────────
        @NotNull(message = "ID do médico não pode ser nulo")
        Long codigoMedico,
        @NotNull(message = "ID da consulta não pode ser nulo")
        Long consulta,

        // ── Identificação do Paciente ─────────────────────────────────────────
        String responsavel,


        String exameOutros,

        // ── Diagnóstico e Tratamento ──────────────────────────────────────────
        String orientacoes,

        // ── TUSS e CID ────────────────────────────────────────────────────────
        String tussTexto,
        String cidTexto,
        String solicitacaoExameTexto
) {
}
