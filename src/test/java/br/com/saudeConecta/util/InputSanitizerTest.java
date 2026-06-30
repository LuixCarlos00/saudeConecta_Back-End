package br.com.saudeConecta.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InputSanitizer - Testes unitários")
class InputSanitizerTest {

    // ========== sanitizeText ==========

    @Nested
    @DisplayName("sanitizeText")
    class SanitizeText {

        @Test
        @DisplayName("Deve retornar null quando input é null")
        void deveRetornarNullParaInputNull() {
            assertThat(InputSanitizer.sanitizeText(null)).isNull();
        }

        @Test
        @DisplayName("Deve remover tags HTML")
        void deveRemoverTagsHtml() {
            assertThat(InputSanitizer.sanitizeText("<b>João</b>")).isEqualTo("João");
        }

        @Test
        @DisplayName("Deve fazer trim no texto")
        void deveFazerTrim() {
            assertThat(InputSanitizer.sanitizeText("  texto  ")).isEqualTo("texto");
        }

        @Test
        @DisplayName("Deve retornar string vazia para input em branco")
        void deveRetornarVazioParaBranco() {
            assertThat(InputSanitizer.sanitizeText("   ")).isEmpty();
        }
    }

    // ========== sanitizeName ==========

    @Nested
    @DisplayName("sanitizeName")
    class SanitizeName {

        @Test
        @DisplayName("Deve retornar null quando input é null")
        void deveRetornarNullParaNull() {
            assertThat(InputSanitizer.sanitizeName(null)).isNull();
        }

        @Test
        @DisplayName("Deve manter letras, espaços e acentos")
        void deveMaterLetrasEAcentos() {
            assertThat(InputSanitizer.sanitizeName("João da Silva")).isEqualTo("João da Silva");
        }

        @Test
        @DisplayName("Deve remover números do nome")
        void deveRemoverNumeros() {
            assertThat(InputSanitizer.sanitizeName("João123")).isEqualTo("João");
        }
    }

    // ========== sanitizeCpf / sanitizeCnpj / sanitizeCep / sanitizeTelefone ==========

    @Nested
    @DisplayName("Sanitização de documentos e contatos")
    class DocumentosContatos {

        @Test
        @DisplayName("sanitizeCpf deve remover pontuação")
        void deveSanitizarCpf() {
            assertThat(InputSanitizer.sanitizeCpf("123.456.789-09")).isEqualTo("12345678909");
        }

        @Test
        @DisplayName("sanitizeCpf deve retornar null para null")
        void deveSanitizarCpfNull() {
            assertThat(InputSanitizer.sanitizeCpf(null)).isNull();
        }

        @Test
        @DisplayName("sanitizeCnpj deve remover formatação")
        void deveSanitizarCnpj() {
            assertThat(InputSanitizer.sanitizeCnpj("12.345.678/0001-90")).isEqualTo("12345678000190");
        }

        @Test
        @DisplayName("sanitizeCep deve remover hífen")
        void deveSanitizarCep() {
            assertThat(InputSanitizer.sanitizeCep("01310-100")).isEqualTo("01310100");
        }

        @Test
        @DisplayName("sanitizeTelefone deve remover formatação")
        void deveSanitizarTelefone() {
            assertThat(InputSanitizer.sanitizeTelefone("(11) 98765-4321")).isEqualTo("11987654321");
        }

        @Test
        @DisplayName("sanitizeEmail deve fazer trim e lowercase")
        void deveSanitizarEmail() {
            assertThat(InputSanitizer.sanitizeEmail("  JOAO@EMAIL.COM  ")).isEqualTo("joao@email.com");
        }

        @Test
        @DisplayName("sanitizeEmail deve retornar null para null")
        void deveSanitizarEmailNull() {
            assertThat(InputSanitizer.sanitizeEmail(null)).isNull();
        }
    }

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

    // ========== isValidCpfFormat / isValidCnpjFormat ==========

    @Nested
    @DisplayName("Validação de formato")
    class ValidacaoFormato {

        @Test
        @DisplayName("CPF com 11 dígitos deve ser válido")
        void cpfValidoDevePassar() {
            assertThat(InputSanitizer.isValidCpfFormat("12345678909")).isTrue();
        }

        @Test
        @DisplayName("CPF formatado deve ser válido após limpeza")
        void cpfFormatadoDevePassar() {
            assertThat(InputSanitizer.isValidCpfFormat("123.456.789-09")).isTrue();
        }

        @Test
        @DisplayName("CPF com dígitos insuficientes deve ser inválido")
        void cpfCurtoDeveSerInvalido() {
            assertThat(InputSanitizer.isValidCpfFormat("1234")).isFalse();
        }

        @Test
        @DisplayName("CPF null deve ser inválido")
        void cpfNullDeveSerInvalido() {
            assertThat(InputSanitizer.isValidCpfFormat(null)).isFalse();
        }

        @Test
        @DisplayName("CNPJ com 14 dígitos deve ser válido")
        void cnpjValidoDevePassar() {
            assertThat(InputSanitizer.isValidCnpjFormat("12345678000190")).isTrue();
        }

        @Test
        @DisplayName("CNPJ formatado deve ser válido após limpeza")
        void cnpjFormatadoDevePassar() {
            assertThat(InputSanitizer.isValidCnpjFormat("12.345.678/0001-90")).isTrue();
        }

        @Test
        @DisplayName("CNPJ null deve ser inválido")
        void cnpjNullDeveSerInvalido() {
            assertThat(InputSanitizer.isValidCnpjFormat(null)).isFalse();
        }
    }
}
