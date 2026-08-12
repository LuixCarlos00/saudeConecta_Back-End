package br.com.saudeConecta.presentation.dto.relatorio;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consulta atendida com os documentos gerados nela.
 *
 * @param id                identificador da consulta
 * @param dataHora          data e hora do atendimento
 * @param status            status atual da consulta
 * @param especialidadeNome especialidade vinculada a consulta
 * @param profissionalId    identificador do profissional responsavel
 * @param profissionalNome  nome do profissional responsavel
 * @param diagnostico       diagnostico registrado no prontuario, quando houver
 * @param documentos        documentos derivados do atendimento
 */
public record ConsultaRelatorioResponse(
        Long id,
        LocalDateTime dataHora,
        String status,
        String especialidadeNome,
        Long profissionalId,
        String profissionalNome,
        String diagnostico,
        List<DocumentoRelatorioResponse> documentos
) {
}
