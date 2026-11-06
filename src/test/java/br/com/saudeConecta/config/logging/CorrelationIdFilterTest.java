package br.com.saudeConecta.config.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdFilter - requestId no MDC e no header de resposta")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    private ListAppender<ILoggingEvent> appenderAtivo;

    @AfterEach
    void limpar() {
        MDC.clear();
        if (appenderAtivo != null) {
            ((Logger) LoggerFactory.getLogger(CorrelationIdFilter.class)).detachAppender(appenderAtivo);
        }
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

    /** Captura o que o filtro loga, com o requestId que estava no MDC no momento da linha. */
    private ListAppender<ILoggingEvent> capturarLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        appenderAtivo = appender;
        return appender;
    }



    @Test
    @DisplayName("Registra 'http fim' com rota padrão, status e duração, ainda com o requestId no MDC")
    void registraFimDaRequisicao() throws Exception {
        ListAppender<ILoggingEvent> log = capturarLog();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/publico/questionario/tok-secreto-123");
        request.addHeader("X-Request-Id", "req-1");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/publico/questionario/{token}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(404));

        assertThat(log.list).hasSize(1);
        ILoggingEvent fim = log.list.get(0);
        assertThat(fim.getLevel()).isEqualTo(Level.INFO);
        assertThat(fim.getFormattedMessage())
                .matches("http fim GET /publico/questionario/\\{token} status=404 duracaoMs=\\d+")
                .doesNotContain("tok-secreto-123");
        assertThat(fim.getMDCPropertyMap()).containsEntry(MdcChaves.REQUEST_ID, "req-1");
    }

    @Test
    @DisplayName("Rota sem padrão do MVC não grava a URI crua; exceção que escapa vira WARN com excecao=sim")
    void naoGravaUriCruaEMarcaExcecao() {
        ListAppender<ILoggingEvent> log = capturarLog();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/qualquer/coisa");
        request.setQueryString("cpf=12345678900");

        try {
            filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                throw new IllegalStateException("falhou");
            });
        } catch (Exception ignorada) {
            // esperado
        }

        ILoggingEvent fim = log.list.get(0);
        assertThat(fim.getLevel()).isEqualTo(Level.WARN);
        assertThat(fim.getFormattedMessage())
                .startsWith("http fim POST (rota-nao-mapeada) status=")
                .contains("excecao=sim")
                .doesNotContain("/qualquer/coisa")
                .doesNotContain("12345678900");
    }

    @Test
    @DisplayName("Preflight de CORS (OPTIONS) não gera linha de fim")
    void ignoraPreflight() throws Exception {
        ListAppender<ILoggingEvent> log = capturarLog();

        filtro.doFilter(new MockHttpServletRequest("OPTIONS", "/consultas"), new MockHttpServletResponse(), (req, res) -> { });

        assertThat(log.list).isEmpty();
    }
}
