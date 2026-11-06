package br.com.saudeConecta.application.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResultadoLogin - só token ou só recusa com mensagem")
class ResultadoLoginTest {

    @Test
    @DisplayName("Autenticado: tem token e não tem recusa")
    void autenticado() {
        ResultadoLogin resultado = ResultadoLogin.autenticado("jwt");

        assertThat(resultado.isAutenticado()).isTrue();
        assertThat(resultado.recusa()).isNull();
    }

    @Test
    @DisplayName("Recusado: não é autenticado e carrega motivo e mensagem")
    void recusado() {
        ResultadoLogin resultado = ResultadoLogin.recusado(ResultadoLogin.Recusa.TIPO_INVALIDO, "Tipo inválido");

        assertThat(resultado.isAutenticado()).isFalse();
        assertThat(resultado.mensagem()).isEqualTo("Tipo inválido");
    }

    @Test
    @DisplayName("Token nulo não vira 'autenticado' nem recusa sem mensagem (evita NPE no Map.of do controller)")
    void tokenNuloRecusado() {
        assertThatThrownBy(() -> ResultadoLogin.autenticado(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResultadoLogin.recusado(ResultadoLogin.Recusa.PLANO_SEM_ACESSO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResultadoLogin.recusado(null, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Token junto com recusa é inválido")
    void tokenComRecusa() {
        assertThatThrownBy(() -> new ResultadoLogin("jwt", ResultadoLogin.Recusa.TIPO_DIVERGENTE, "m"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
