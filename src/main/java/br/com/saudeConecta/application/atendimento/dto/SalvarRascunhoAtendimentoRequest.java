package br.com.saudeConecta.application.atendimento.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Corpo do {@code PUT /atendimentos/{consultaId}/rascunho}.
 *
 * <p>Sem Bean Validation de propósito: o contrato pede 422 (não 400) para {@code tipo} inválido e
 * {@code dados} ausente, então a validação fica no service. {@code tipo} chega como texto para que um
 * valor desconhecido não vire 400 de JSON ilegível.</p>
 *
 * @param tipo                   {@code MEDICO} ou {@code DENTISTA}
 * @param dados                  JSON opaco do front (obrigatório, até 2 MB serializado)
 * @param tempoAcumuladoSegundos cronômetro em segundos (null = 0; negativo = 422)
 * @param versao                 última versão recebida pelo front; null na primeira gravação
 */
public record SalvarRascunhoAtendimentoRequest(
        String tipo,
        JsonNode dados,
        Integer tempoAcumuladoSegundos,
        Long versao
) {
}
