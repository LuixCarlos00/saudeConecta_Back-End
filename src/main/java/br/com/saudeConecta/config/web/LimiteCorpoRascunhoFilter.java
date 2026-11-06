package br.com.saudeConecta.config.web;

import br.com.saudeConecta.presentation.erro.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Barra corpo grande demais em {@code PUT /atendimentos/{id}/rascunho} ANTES de o Jackson montar o JSON
 * (sem isso, um corpo enorme ocupa memória inteira antes de a checagem de 2 MB do service rodar).
 *
 * <p>O limite aqui (2,5 MB) é folga sobre os 2 MB de {@code dados}, que o service continua medindo no JSON
 * real. Com {@code Content-Length} acima do limite responde 413 sem ler nada. Sem {@code Content-Length}
 * (chunked), lê no máximo limite+1 byte, contando; passou disso, 413; senão repassa o corpo já lido.
 * Roda logo depois do {@code CorrelationIdFilter}, então o 413 também tem requestId e linha de {@code http fim}.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class LimiteCorpoRascunhoFilter extends OncePerRequestFilter {

    /** 2,5 MB: os 2 MB de {@code dados} mais o envelope (tipo, versão, tempo) e escapes do JSON. */
    public static final long LIMITE_CORPO_BYTES = 2_621_440L;

    public static final String CODE_MUITO_GRANDE = "RASCUNHO_MUITO_GRANDE";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final String PADRAO = "/atendimentos/*/rascunho";

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"PUT".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return !MATCHER.match(PADRAO, caminho);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long declarado = request.getContentLengthLong();
        if (declarado > LIMITE_CORPO_BYTES) {
            recusar(request, response, "CONTENT_LENGTH", declarado);
            return;
        }
        if (declarado >= 0) {
            chain.doFilter(request, response);
            return;
        }
        byte[] corpo = lerComLimite(request.getInputStream());
        if (corpo == null) {
            recusar(request, response, "CORPO_SEM_CONTENT_LENGTH", -1);
            return;
        }
        chain.doFilter(new CorpoLidoRequest(request, corpo), response);
    }

    /** Lê o corpo contando os bytes; devolve null assim que passar do limite (sem ler o resto). */
    private static byte[] lerComLimite(InputStream in) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int lidos;
        while ((lidos = in.read(buffer)) != -1) {
            total += lidos;
            if (total > LIMITE_CORPO_BYTES) {
                return null;
            }
            saida.write(buffer, 0, lidos);
        }
        return saida.toByteArray();
    }

    private void recusar(HttpServletRequest request, HttpServletResponse response, String motivo, long contentLength)
            throws IOException {
        log.warn("rascunho.salvar negado motivo=MUITO_GRANDE origem={} contentLength={} limiteBytes={}",
                motivo, contentLength, LIMITE_CORPO_BYTES);
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Connection", "close");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                HttpStatus.PAYLOAD_TOO_LARGE.value(), CODE_MUITO_GRANDE,
                "Rascunho acima do limite de 2 MB", request.getRequestURI()));
    }

    /** Repassa ao resto da cadeia o corpo que o filtro já consumiu para contar. */
    private static final class CorpoLidoRequest extends HttpServletRequestWrapper {
        private final byte[] corpo;

        CorpoLidoRequest(HttpServletRequest request, byte[] corpo) {
            super(request);
            this.corpo = corpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream origem = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return origem.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return origem.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return origem.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Leitura assincrona nao suportada");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return corpo.length;
        }

        @Override
        public long getContentLengthLong() {
            return corpo.length;
        }
    }
}
