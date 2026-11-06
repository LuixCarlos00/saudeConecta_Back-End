package br.com.saudeConecta.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Correlation id: liga todas as linhas de log de uma mesma requisição.
 *
 * <p>Roda antes de tudo (inclusive do Spring Security) para que até o log de JWT inválido tenha o
 * requestId. Reaproveita o {@code X-Request-Id} do cliente/proxy quando ele é seguro, senão gera um
 * UUID, e devolve o valor no header da resposta. Por ser o filtro mais externo, é ele quem limpa
 * o MDC inteiro (inclusive tenantId/userId postos pelos filtros de segurança) ao final.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";

    /** Só aceita ids curtos e "limpos": evita log injection e header gigante vindo do cliente. */
    private static final Pattern ID_SEGURO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader(HEADER);
        String requestId = recebido != null && ID_SEGURO.matcher(recebido).matches()
                ? recebido
                : UUID.randomUUID().toString();

        MDC.put(MdcChaves.REQUEST_ID, requestId);
        response.setHeader(HEADER, requestId);
        long inicio = System.nanoTime();
        boolean escapouExcecao = true;
        try {
            chain.doFilter(request, response);
            escapouExcecao = false;
        } finally {
            registrarFim(request, response, inicio, escapouExcecao);
            MDC.clear();
        }
    }

    /**
     * Linha única de fim da requisição (padrão em docs/LOGGING.md): com ela, um requestId que
     * tem "inicio" de uma operação e nenhum "http fim" mostra exatamente onde a requisição parou.
     * Usa o padrão da rota ("/publico/questionario/{token}") e não a URI, para não gravar tokens
     * de links públicos nem ids soltos. Preflight de CORS (OPTIONS) não é registrado.
     *
     * @param request        requisição atendida
     * @param response       resposta já preenchida pela cadeia
     * @param inicioNanos    instante de entrada no filtro ({@link System#nanoTime()})
     * @param escapouExcecao true se a cadeia terminou lançando exceção (o status pode não refletir o erro)
     */
    private static void registrarFim(HttpServletRequest request, HttpServletResponse response,
                                     long inicioNanos, boolean escapouExcecao) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        long duracaoMs = (System.nanoTime() - inicioNanos) / 1_000_000;
        if (escapouExcecao) {
            log.warn("http fim {} {} status={} duracaoMs={} excecao=sim",
                    request.getMethod(), rota(request), response.getStatus(), duracaoMs);
        } else {
            log.info("http fim {} {} status={} duracaoMs={}",
                    request.getMethod(), rota(request), response.getStatus(), duracaoMs);
        }
    }

    /** Padrão da rota resolvido pelo Spring MVC; sem ele (404, recurso estático), só o caminho sem query. */
    private static String rota(HttpServletRequest request) {
        Object padrao = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return padrao != null ? padrao.toString() : ROTA_NAO_MAPEADA;
    }

    /** Rota sem padrão do MVC: não grava a URI crua, que pode conter token ou dado digitado. */
    private static final String ROTA_NAO_MAPEADA = "(rota-nao-mapeada)";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
}
