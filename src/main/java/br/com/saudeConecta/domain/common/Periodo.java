package br.com.saudeConecta.domain.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Intervalo fechado de datas, do primeiro ao último dia, inclusive, com os limites em
 * {@link LocalDateTime} que as consultas ao banco usam.
 *
 * <p>Centraliza o cálculo de "dia", "semana", "mês" e "ano" que antes era repetido à mão nos
 * services. A semana vai de segunda a domingo. O fim padrão é 23:59:59, o mesmo das consultas
 * {@code BETWEEN} existentes: a coluna {@code data_hora} não guarda fração de segundo.
 *
 * @param dataInicio primeiro dia do período (inclusive)
 * @param dataFim    último dia do período (inclusive)
 */
public record Periodo(LocalDate dataInicio, LocalDate dataFim) {

    private static final LocalTime ULTIMO_SEGUNDO = LocalTime.of(23, 59, 59);

    /**
     * Valida que as duas datas foram informadas.
     *
     * @param dataInicio primeiro dia do período
     * @param dataFim    último dia do período
     */
    public Periodo {
        Objects.requireNonNull(dataInicio, "dataInicio");
        Objects.requireNonNull(dataFim, "dataFim");
    }

    /**
     * Período de um único dia.
     *
     * @param data o dia
     * @return período que começa e termina em {@code data}
     */
    public static Periodo dia(LocalDate data) {
        return new Periodo(data, data);
    }

    /**
     * Semana, de segunda a domingo, que contém a data.
     *
     * @param data qualquer dia da semana desejada
     * @return período da segunda-feira ao domingo dessa semana
     */
    public static Periodo semanaDe(LocalDate data) {
        LocalDate segunda = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Periodo(segunda, segunda.plusDays(6));
    }

    /**
     * Mês civil que contém a data.
     *
     * @param data qualquer dia do mês desejado
     * @return período do dia 1 ao último dia do mês
     */
    public static Periodo mesDe(LocalDate data) {
        return new Periodo(data.withDayOfMonth(1), data.withDayOfMonth(data.lengthOfMonth()));
    }

    /**
     * Ano civil que contém a data.
     *
     * @param data qualquer dia do ano desejado
     * @return período de 1º de janeiro a 31 de dezembro
     */
    public static Periodo anoDe(LocalDate data) {
        return new Periodo(data.withDayOfYear(1), data.withDayOfYear(data.lengthOfYear()));
    }

    /**
     * Período entre duas datas informadas pelo usuário, sem ajuste.
     *
     * @param dataInicio primeiro dia (inclusive)
     * @param dataFim    último dia (inclusive)
     * @return o período
     */
    public static Periodo entre(LocalDate dataInicio, LocalDate dataFim) {
        return new Periodo(dataInicio, dataFim);
    }

    /**
     * Início do primeiro dia (00:00:00).
     *
     * @return limite inferior, inclusive
     */
    public LocalDateTime inicio() {
        return dataInicio.atStartOfDay();
    }

    /**
     * Último segundo do último dia (23:59:59), para consultas {@code BETWEEN} inclusivas.
     *
     * @return limite superior, inclusive
     */
    public LocalDateTime fim() {
        return dataFim.atTime(ULTIMO_SEGUNDO);
    }

    /**
     * Meia-noite do dia seguinte ao último, para consultas com {@code < fim}.
     *
     * @return limite superior, exclusivo
     */
    public LocalDateTime fimExclusivo() {
        return dataFim.plusDays(1).atStartOfDay();
    }

    /**
     * Último instante representável do último dia (23:59:59.999999999). Existe para o saldo
     * financeiro, que sempre usou esse limite; com a coluna sem fração de segundo, o resultado
     * é o mesmo de {@link #fim()}.
     *
     * @return limite superior, inclusive
     */
    public LocalDateTime ultimoInstante() {
        return dataFim.atTime(LocalTime.MAX);
    }
}
