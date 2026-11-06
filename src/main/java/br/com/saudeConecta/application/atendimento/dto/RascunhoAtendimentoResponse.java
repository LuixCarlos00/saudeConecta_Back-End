package br.com.saudeConecta.application.atendimento.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * Rascunho do atendimento devolvido pelo GET e pelo PUT.
 *
 * @param consultaId             ID da consulta
 * @param tipo                   {@code MEDICO} ou {@code DENTISTA}
 * @param dados                  JSON exatamente como foi salvo (objeto, não string)
 * @param tempoAcumuladoSegundos cronômetro em segundos
 * @param versao                 versão atual, a ser enviada no próximo PUT
 * @param atualizadoEm           momento da última gravação
 */
public record RascunhoAtendimentoResponse(
        Long consultaId,
        String tipo,
        JsonNode dados,
        Integer tempoAcumuladoSegundos,
        Long versao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime atualizadoEm
) {
}
