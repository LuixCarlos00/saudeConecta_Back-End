package br.com.saudeConecta.domain.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Periodo - cálculo de dia, semana, mês e ano")
class PeriodoTest {

    private static LocalDate data(String iso) {
        return LocalDate.parse(iso);
    }

    @Nested
    @DisplayName("dia")
    class Dia {

        @Test
        @DisplayName("vai de 00:00:00 a 23:59:59 do mesmo dia")
        void limitesDoDia() {
            Periodo dia = Periodo.dia(data("2026-11-06"));

            assertThat(dia.dataInicio()).isEqualTo(data("2026-11-06"));
            assertThat(dia.dataFim()).isEqualTo(data("2026-11-06"));
            assertThat(dia.inicio()).isEqualTo(LocalDateTime.of(2026, 11, 6, 0, 0, 0));
            assertThat(dia.fim()).isEqualTo(LocalDateTime.of(2026, 11, 6, 23, 59, 59));
        }

        @Test
        @DisplayName("fim exclusivo é a meia-noite do dia seguinte, inclusive na virada de ano")
        void fimExclusivoViraOAno() {
            Periodo dia = Periodo.dia(data("2026-12-31"));

            assertThat(dia.fimExclusivo()).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0, 0));
        }

        @Test
        @DisplayName("último instante é 23:59:59.999999999")
        void ultimoInstante() {
            assertThat(Periodo.dia(data("2026-11-06")).ultimoInstante())
                    .isEqualTo(LocalDateTime.of(data("2026-11-06"), LocalTime.MAX));
        }
    }

    @Nested
    @DisplayName("semanaDe")
    class SemanaDe {

        @Test
        @DisplayName("semana começa na segunda e termina no domingo")
        void segundaADomingo() {
            Periodo semana = Periodo.semanaDe(data("2026-11-06")); // sexta-feira

            assertThat(semana.dataInicio()).isEqualTo(data("2026-11-02")); // segunda
            assertThat(semana.dataFim()).isEqualTo(data("2026-11-08"));    // domingo
            assertThat(semana.inicio()).isEqualTo(LocalDateTime.of(2026, 11, 2, 0, 0, 0));
            assertThat(semana.fim()).isEqualTo(LocalDateTime.of(2026, 11, 8, 23, 59, 59));
        }

        @Test
        @DisplayName("na própria segunda, a semana começa nela mesma")
        void naSegunda() {
            assertThat(Periodo.semanaDe(data("2026-11-02")).dataInicio()).isEqualTo(data("2026-11-02"));
        }

        @Test
        @DisplayName("no domingo, a semana é a que começou na segunda anterior")
        void noDomingo() {
            Periodo semana = Periodo.semanaDe(data("2026-11-08"));

            assertThat(semana.dataInicio()).isEqualTo(data("2026-11-02"));
            assertThat(semana.dataFim()).isEqualTo(data("2026-11-08"));
        }

        @Test
        @DisplayName("semana que atravessa a virada de mês")
        void viradaDeMes() {
            Periodo semana = Periodo.semanaDe(data("2026-07-29")); // quarta

            assertThat(semana.dataInicio()).isEqualTo(data("2026-07-27"));
            assertThat(semana.dataFim()).isEqualTo(data("2026-08-02"));
        }

        @Test
        @DisplayName("semana que atravessa a virada de ano, pelos dois lados")
        void viradaDeAno() {
            Periodo pelaQuinta = Periodo.semanaDe(data("2026-12-31"));
            Periodo pelaSexta = Periodo.semanaDe(data("2027-01-01"));

            assertThat(pelaQuinta.dataInicio()).isEqualTo(data("2026-12-28"));
            assertThat(pelaQuinta.dataFim()).isEqualTo(data("2027-01-03"));
            assertThat(pelaSexta).isEqualTo(pelaQuinta);
        }

        @Test
        @DisplayName("é igual à conta antiga dos services (data - (diaDaSemana - 1)) em todos os dias de um ano")
        void mesmaRegraDaContaAntiga() {
            for (LocalDate d = data("2026-01-01"); d.getYear() == 2026; d = d.plusDays(1)) {
                LocalDate segundaAntiga = d.minusDays(d.getDayOfWeek().getValue() - 1);
                Periodo semana = Periodo.semanaDe(d);
                assertThat(semana.dataInicio()).as("dia %s", d).isEqualTo(segundaAntiga);
                assertThat(semana.dataFim()).as("dia %s", d).isEqualTo(segundaAntiga.plusDays(6));
            }
        }
    }

    @Nested
    @DisplayName("mesDe")
    class MesDe {

        @Test
        @DisplayName("vai do dia 1 ao último dia do mês")
        void mesComum() {
            Periodo mes = Periodo.mesDe(data("2026-11-06"));

            assertThat(mes.dataInicio()).isEqualTo(data("2026-11-01"));
            assertThat(mes.dataFim()).isEqualTo(data("2026-11-30"));
            assertThat(mes.fim()).isEqualTo(LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        }

        @Test
        @DisplayName("fevereiro respeita ano bissexto")
        void fevereiro() {
            assertThat(Periodo.mesDe(data("2026-02-10")).dataFim()).isEqualTo(data("2026-02-28"));
            assertThat(Periodo.mesDe(data("2028-02-10")).dataFim()).isEqualTo(data("2028-02-29"));
        }

        @Test
        @DisplayName("dezembro termina em 31/12 e o fim exclusivo cai no ano seguinte")
        void dezembro() {
            Periodo mes = Periodo.mesDe(data("2026-12-15"));

            assertThat(mes.dataFim()).isEqualTo(data("2026-12-31"));
            assertThat(mes.fimExclusivo()).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0, 0));
        }
    }

    @Nested
    @DisplayName("anoDe e entre")
    class AnoEEntre {

        @Test
        @DisplayName("ano vai de 01/01 a 31/12")
        void ano() {
            Periodo ano = Periodo.anoDe(data("2028-06-15"));

            assertThat(ano.dataInicio()).isEqualTo(data("2028-01-01"));
            assertThat(ano.dataFim()).isEqualTo(data("2028-12-31"));
        }

        @Test
        @DisplayName("entre mantém as datas informadas sem ajuste")
        void entre() {
            Periodo periodo = Periodo.entre(data("2026-10-15"), data("2026-11-03"));

            assertThat(periodo.inicio()).isEqualTo(LocalDateTime.of(2026, 10, 15, 0, 0, 0));
            assertThat(periodo.fim()).isEqualTo(LocalDateTime.of(2026, 11, 3, 23, 59, 59));
            assertThat(periodo.fimExclusivo()).isEqualTo(LocalDateTime.of(2026, 11, 4, 0, 0, 0));
        }

        @Test
        @DisplayName("data nula é recusada na criação")
        void dataNula() {
            var fim = data("2026-11-03");
            assertThatThrownBy(() -> Periodo.entre(null, fim))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("dataInicio");
            assertThatThrownBy(() -> Periodo.dia(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
