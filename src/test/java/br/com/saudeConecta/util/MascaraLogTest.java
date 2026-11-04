package br.com.saudeConecta.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MascaraLog - mascaramento de dados pessoais e credenciais em log")
class MascaraLogTest {

    @Test
    @DisplayName("Token: mantém no máximo os 6 primeiros caracteres")
    void tokenMantemSeisPrimeiros() {
        assertThat(MascaraLog.token("123e4567-e89b-12d3-a456-426614174000")).isEqualTo("123e45***");
    }

    @Test
    @DisplayName("Token curto e nulo não quebram")
    void tokenCurtoENulo() {
        assertThat(MascaraLog.token("abc")).isEqualTo("***");
        assertThat(MascaraLog.token(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("E-mail: mantém a primeira letra e o domínio")
    void emailMantemPrimeiraLetraEDominio() {
        assertThat(MascaraLog.email("fulano@clinica.com")).isEqualTo("f***@clinica.com");
    }

    @Test
    @DisplayName("E-mail vazio, nulo ou sem arroba não vaza o valor")
    void emailInvalido() {
        assertThat(MascaraLog.email(null)).isEqualTo("<vazio>");
        assertThat(MascaraLog.email("  ")).isEqualTo("<vazio>");
        assertThat(MascaraLog.email("semarroba")).isEqualTo("***");
        assertThat(MascaraLog.email("@dominio.com")).isEqualTo("***");
    }

    @Test
    @DisplayName("CPF: mostra só os 2 últimos dígitos, com ou sem máscara de entrada")
    void cpfMostraUltimosDois() {
        assertThat(MascaraLog.cpf("123.456.789-01")).isEqualTo("***.***.***-01");
        assertThat(MascaraLog.cpf("12345678901")).isEqualTo("***.***.***-01");
    }

    @Test
    @DisplayName("CPF nulo ou curto não quebra")
    void cpfNuloOuCurto() {
        assertThat(MascaraLog.cpf(null)).isEqualTo("null");
        assertThat(MascaraLog.cpf("1")).isEqualTo("***");
    }
}
