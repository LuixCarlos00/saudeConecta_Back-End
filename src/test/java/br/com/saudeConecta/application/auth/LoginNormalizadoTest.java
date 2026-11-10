package br.com.saudeConecta.application.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginNormalizado - CPF/CNPJ com ou sem máscara")
class LoginNormalizadoTest {

    @ParameterizedTest(name = "[{0}] -> [{1}]")
    @CsvSource(delimiter = '|', value = {
            "107.407.590-00      | 10740759000",
            "10740759000         | 10740759000",
            "107407590-00        | 10740759000",
            "'  107.407.590-00 ' | 10740759000",
            "12.345.678/0001-90  | 12345678000190",
            "12345678000190      | 12345678000190",
            "12345678/0001-90    | 12345678000190",
            "123.456             | 123.456",
            "1074075900          | 1074075900",
            "admin               | admin",
            "'  root  '          | root",
            "joao.silva          | joao.silva"
    })
    @DisplayName("Tira a máscara só de CPF (11) e CNPJ (14); outros logins ficam como vieram, sem espaços nas pontas")
    void normaliza(String digitado, String esperado) {
        assertThat(LoginNormalizado.normalizar(digitado)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Login nulo continua nulo")
    void nulo(String digitado) {
        assertThat(LoginNormalizado.normalizar(digitado)).isNull();
    }
}
