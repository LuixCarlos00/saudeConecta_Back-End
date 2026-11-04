package br.com.saudeConecta.infra.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
