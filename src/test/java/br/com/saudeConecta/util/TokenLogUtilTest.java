package br.com.saudeConecta.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenLogUtil - mascaramento de tokens públicos em log")
class TokenLogUtilTest {

    @Test
    @DisplayName("Mantém no máximo os 6 primeiros caracteres")
    void mantemSeisPrimeiros() {
        assertThat(TokenLogUtil.mascarar("123e4567-e89b-12d3-a456-426614174000")).isEqualTo("123e45***");
    }

    @Test
    @DisplayName("Token curto e nulo não quebram")
    void curtoENulo() {
        assertThat(TokenLogUtil.mascarar("abc")).isEqualTo("***");
        assertThat(TokenLogUtil.mascarar(null)).isEqualTo("null");
    }
}
