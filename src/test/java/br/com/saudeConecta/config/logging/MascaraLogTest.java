package br.com.saudeConecta.config.logging;

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

    @Test
    @DisplayName("CEP: mantém os 5 primeiros dígitos, com ou sem hífen")
    void cepMantemCincoPrimeiros() {
        assertThat(MascaraLog.cep("01310-100")).isEqualTo("01310***");
        assertThat(MascaraLog.cep("01310100")).isEqualTo("01310***");
    }

    @Test
    @DisplayName("CEP curto e nulo não quebram nem vazam")
    void cepCurtoENulo() {
        assertThat(MascaraLog.cep("123")).isEqualTo("***");
        assertThat(MascaraLog.cep(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("seguro: troca CR/LF e caracteres fora de [0-9A-Za-z._:-] por '_'")
    void seguroNeutralizaQuebraDeLinha() {
        assertThat(MascaraLog.seguro("123\r\n2026 INFO forjado")).isEqualTo("123__2026_INFO_forjado");
        assertThat(MascaraLog.seguro("1704908010")).isEqualTo("1704908010");
        assertThat(MascaraLog.seguro("a.b_c:d-e")).isEqualTo("a.b_c:d-e");
    }

    @Test
    @DisplayName("seguro: limita a 32 caracteres, aceita limite informado e trata nulo")
    void seguroLimitaTamanho() {
        assertThat(MascaraLog.seguro("x".repeat(100))).hasSize(32);
        assertThat(MascaraLog.seguro("x".repeat(100), 120)).hasSize(100);
        assertThat(MascaraLog.seguro("x".repeat(200), 120)).hasSize(120);
        assertThat(MascaraLog.seguro(null)).isEqualTo("null");
    }
}
