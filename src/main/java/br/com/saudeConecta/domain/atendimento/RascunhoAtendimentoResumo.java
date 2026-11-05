package br.com.saudeConecta.domain.atendimento;

import java.time.LocalDateTime;

/**
 * Projeção leve do rascunho para a agenda ("Retomar atendimento"): não carrega {@code dados},
 * que pode ter até 2 MB por consulta.
 */
public interface RascunhoAtendimentoResumo {

    /** @return ID da consulta do rascunho */
    Long getConsultaId();

    /** @return momento da última gravação */
    LocalDateTime getAtualizadoEm();

    /** @return tempo do cronômetro já acumulado, em segundos */
    Integer getTempoAcumuladoSegundos();
}
