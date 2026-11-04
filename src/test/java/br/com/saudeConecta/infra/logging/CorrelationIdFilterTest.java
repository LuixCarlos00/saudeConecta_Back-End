package br.com.saudeConecta.infra.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdFilter - requestId no MDC e no header de resposta")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @AfterEach
    void limpar() {
        MDC.clear();
    }

    @Test
    @DisplayName("Reaproveita o X-Request-Id recebido, devolve no header e põe no MDC durante a requisição")
    void reaproveitaHeaderRecebido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/consultas");
        request.addHeader("X-Request-Id", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> noMdc = new AtomicReference<>();

        filtro.doFilter(request, response, (req, res) -> noMdc.set(MDC.get(MdcChaves.REQUEST_ID)));

        assertThat(noMdc.get()).isEqualTo("abc-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("Gera UUID quando o header não vem")
    void geraQuandoAusente() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> noMdc = new AtomicReference<>();

        filtro.doFilter(new MockHttpServletRequest("GET", "/x"), response,
                (req, res) -> noMdc.set(MDC.get(MdcChaves.REQUEST_ID)));

        assertThat(noMdc.get()).isNotBlank().hasSize(36);
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(noMdc.get());
    }

    @Test
    @DisplayName("Header forjado (grande ou com caracteres de controle) é descartado")
    void descartaHeaderInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Request-Id", "linha\nforjada");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("X-Request-Id")).doesNotContain("\n").hasSize(36);
    }

    @Test
    @DisplayName("Limpa o MDC (requestId, tenantId, userId) ao final, mesmo com exceção")
    void limpaMdcNoFinally() {
        try {
            filtro.doFilter(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), (req, res) -> {
                MDC.put(MdcChaves.TENANT_ID, "7");
                MDC.put(MdcChaves.USER_ID, "1");
                throw new IllegalStateException("falhou");
            });
        } catch (Exception ignorada) {
            // esperado
        }

        assertThat(MDC.get(MdcChaves.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcChaves.TENANT_ID)).isNull();
        assertThat(MDC.get(MdcChaves.USER_ID)).isNull();
    }
}
