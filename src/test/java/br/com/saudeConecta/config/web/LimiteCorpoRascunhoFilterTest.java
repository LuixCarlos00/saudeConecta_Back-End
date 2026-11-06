package br.com.saudeConecta.config.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LimiteCorpoRascunhoFilter - 413 antes de o Jackson ler o corpo do PUT do rascunho")
class LimiteCorpoRascunhoFilterTest {

    private static final int LIMITE = (int) LimiteCorpoRascunhoFilter.LIMITE_CORPO_BYTES;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final LimiteCorpoRascunhoFilter filtro = new LimiteCorpoRascunhoFilter(objectMapper);

    /** Request cujo Content-Length é controlado pelo teste (-1 = chunked, sem o header). */
    private MockHttpServletRequest put(String uri, byte[] corpo, long contentLength) {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", uri) {
            @Override
            public long getContentLengthLong() {
                return contentLength;
            }

            @Override
            public int getContentLength() {
                return (int) contentLength;
            }
        };
        request.setContent(corpo);
        return request;
    }

    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        Arrays.fill(b, (byte) 'a');
        return b;
    }

    @Test
    @DisplayName("Content-Length acima de 2,5 MB: 413 RASCUNHO_MUITO_GRANDE sem chamar o resto da cadeia")
    void contentLengthGrande() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chamou = new AtomicBoolean();

        filtro.doFilter(put("/atendimentos/822/rascunho", bytes(10), LIMITE + 1L), response,
                (req, res) -> chamou.set(true));

        assertThat(response.getStatus()).isEqualTo(413);
        JsonNode corpo = objectMapper.readTree(response.getContentAsString());
        assertThat(corpo.get("code").asText()).isEqualTo("RASCUNHO_MUITO_GRANDE");
        assertThat(corpo.get("status").asInt()).isEqualTo(413);
        assertThat(corpo.get("message").asText()).isNotBlank();
        assertThat(chamou).isFalse();
    }

    @Test
    @DisplayName("Sem Content-Length e corpo passando de 2,5 MB: 413, contando ao ler")
    void corpoSemContentLengthGrande() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chamou = new AtomicBoolean();

        filtro.doFilter(put("/atendimentos/822/rascunho", bytes(LIMITE + 1), -1), response,
                (req, res) -> chamou.set(true));

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
                .isEqualTo("RASCUNHO_MUITO_GRANDE");
        assertThat(chamou).isFalse();
    }

    @Test
    @DisplayName("Sem Content-Length e corpo dentro do limite: repassa o corpo inteiro para a cadeia")
    void corpoSemContentLengthDentroDoLimite() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> lido = new AtomicReference<>();
        byte[] corpo = bytes(LIMITE);

        filtro.doFilter(put("/atendimentos/822/rascunho", corpo, -1), response,
                (req, res) -> lido.set(req.getInputStream().readAllBytes()));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(lido.get()).isEqualTo(corpo);
    }

    @Test
    @DisplayName("Content-Length dentro do limite passa direto, sem reler o corpo")
    void contentLengthNormal() throws Exception {
        AtomicBoolean chamou = new AtomicBoolean();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(put("/atendimentos/822/rascunho", bytes(100), 100), response, (req, res) -> chamou.set(true));

        assertThat(chamou).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Só vale para PUT /atendimentos/{id}/rascunho: outras rotas e métodos não são tocados")
    void outrasRotasIntactas() throws Exception {
        AtomicBoolean chamou = new AtomicBoolean();

        filtro.doFilter(put("/prontuario/cadastrarProntuarioMedico", bytes(10), LIMITE + 1L),
                new MockHttpServletResponse(), (req, res) -> chamou.set(true));
        assertThat(chamou).isTrue();

        chamou.set(false);
        MockHttpServletRequest post = put("/atendimentos/822/rascunho", bytes(10), LIMITE + 1L);
        post.setMethod("POST");
        filtro.doFilter(post, new MockHttpServletResponse(), (req, res) -> chamou.set(true));
        assertThat(chamou).isTrue();
    }
}
