package br.com.saudeConecta.config.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InputSanitizer - Testes unitários")
class InputSanitizerTest {

    // ========== SQL Injection ==========

    @Nested
    @DisplayName("containsSqlInjection")
    class SqlInjection {

        @Test
        @DisplayName("Deve detectar SELECT")
        void deveDetectarSelect() {
            assertThat(InputSanitizer.containsSqlInjection("SELECT * FROM users")).isTrue();
        }

        @Test
        @DisplayName("Deve detectar DROP TABLE")
        void deveDetectarDrop() {
            assertThat(InputSanitizer.containsSqlInjection("DROP TABLE clientes")).isTrue();
        }

        @Test
        @DisplayName("Deve detectar comentário SQL --")
        void deveDetectarComentarioSql() {
            assertThat(InputSanitizer.containsSqlInjection("texto -- comentario")).isTrue();
        }

        @Test
        @DisplayName("Texto normal não deve ser detectado como SQL injection")
        void textoNormalNaoDeveSerDetectado() {
            assertThat(InputSanitizer.containsSqlInjection("João da Silva")).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para input null")
        void deveRetornarFalseParaNull() {
            assertThat(InputSanitizer.containsSqlInjection(null)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para input em branco")
        void deveRetornarFalseParaBranco() {
            assertThat(InputSanitizer.containsSqlInjection("   ")).isFalse();
        }
    }

    // ========== XSS ==========

    @Nested
    @DisplayName("containsXss")
    class Xss {

        @Test
        @DisplayName("Deve detectar tag script")
        void deveDetectarScript() {
            assertThat(InputSanitizer.containsXss("<script>alert('xss')</script>")).isTrue();
        }

        @Test
        @DisplayName("Deve detectar javascript:")
        void deveDetectarJavascript() {
            assertThat(InputSanitizer.containsXss("javascript:alert(1)")).isTrue();
        }

        @Test
        @DisplayName("Texto normal não deve ser detectado como XSS")
        void textoNormalNaoDeveSerDetectado() {
            assertThat(InputSanitizer.containsXss("texto seguro")).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para null")
        void deveRetornarFalseParaNull() {
            assertThat(InputSanitizer.containsXss(null)).isFalse();
        }
    }

    // ========== isSafe ==========

    @Nested
    @DisplayName("isSafe")
    class IsSafe {

        @Test
        @DisplayName("Texto seguro deve retornar true")
        void textoSeguroDeveRetornarTrue() {
            assertThat(InputSanitizer.isSafe("Nome do paciente")).isTrue();
        }

        @Test
        @DisplayName("SQL injection deve retornar false")
        void sqlInjectionDeveRetornarFalse() {
            assertThat(InputSanitizer.isSafe("'; DROP TABLE users; --")).isFalse();
        }

        @Test
        @DisplayName("XSS deve retornar false")
        void xssDeveRetornarFalse() {
            assertThat(InputSanitizer.isSafe("<script>xss</script>")).isFalse();
        }

        @Test
        @DisplayName("Null deve ser considerado seguro")
        void nullDeveSerSeguro() {
            assertThat(InputSanitizer.isSafe(null)).isTrue();
        }
    }
}
