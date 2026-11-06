package br.com.saudeConecta.application.consulta.dto;

/**
 * Horário ocupado na agenda de um profissional.
 *
 * @param inicio         hora de início no formato HH:mm:ss (mesmo formato de /horarios-ocupados)
 * @param duracaoMinutos duração da consulta em minutos (30 quando a consulta não tem duração)
 */
public record HorarioOcupadoResponse(String inicio, int duracaoMinutos) {}
