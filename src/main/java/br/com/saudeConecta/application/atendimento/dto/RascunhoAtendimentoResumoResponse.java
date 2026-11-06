package br.com.saudeConecta.application.atendimento.dto;

import br.com.saudeConecta.domain.atendimento.RascunhoAtendimentoResumo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Item do {@code GET /atendimentos/rascunhos/meus}: o suficiente para a agenda mostrar "Retomar atendimento".
 *
 * @param consultaId             ID da consulta
 * @param atualizadoEm           momento da última gravação
 * @param tempoAcumuladoSegundos cronômetro em segundos
 */
public record RascunhoAtendimentoResumoResponse(
        Long consultaId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime atualizadoEm,
        Integer tempoAcumuladoSegundos
) {

    /**
     * Converte a projeção do repositório.
     *
     * @param resumo projeção sem {@code dados}
     * @return item da resposta
     */
    public static RascunhoAtendimentoResumoResponse de(RascunhoAtendimentoResumo resumo) {
        return new RascunhoAtendimentoResumoResponse(
                resumo.getConsultaId(), resumo.getAtualizadoEm(), resumo.getTempoAcumuladoSegundos());
    }
}
