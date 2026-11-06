package br.com.saudeConecta.integration;

import br.com.saudeConecta.application.prontuario.PlanejamentoTerapeuticoService;
import br.com.saudeConecta.application.prontuario.TermoAutorizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("QuestionarioPublicoController - IP de origem e tokens fora do log")
class QuestionarioPublicoControllerIntegrationTest {

    private static final String TOKEN = "abcdef-1234-5678-9999";

    @Autowired private MockMvc mockMvc;

    @MockBean private TermoAutorizacaoService termoService;
    @MockBean private PlanejamentoTerapeuticoService planejamentoService;

    @Test
    @DisplayName("Assinatura do planejamento ignora X-Forwarded-For enviado pelo cliente e não loga o token")
    void assinarPlanejamentoIgnoraXffForjado(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/publico/planejamento/assinar")
                        .with(req -> { req.setRemoteAddr("10.1.2.3"); return req; })
                        .header("X-Forwarded-For", "6.6.6.6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\",\"assinaturaBase64\":\"sig\"}"))
                .andExpect(status().isOk());

        verify(planejamentoService).assinarPorToken(TOKEN, "sig", "10.1.2.3");
        assertThat(output.getAll()).contains("token=abcdef***").doesNotContain(TOKEN);
    }

    @Test
    @DisplayName("Resposta do questionário ignora X-Forwarded-For enviado pelo cliente e não loga o token")
    void responderQuestionarioIgnoraXffForjado(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/publico/questionario/responder")
                        .with(req -> { req.setRemoteAddr("10.1.2.3"); return req; })
                        .header("X-Forwarded-For", "6.6.6.6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isOk());

        verify(termoService).responderQuestionario(any(), eq("10.1.2.3"));
        assertThat(output.getAll()).contains("token=abcdef***").doesNotContain(TOKEN);
    }

    @Test
    @DisplayName("GETs públicos não logam o token inteiro")
    void getsNaoLogamToken(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/publico/questionario/" + TOKEN));
        mockMvc.perform(get("/publico/planejamento/" + TOKEN));

        assertThat(output.getAll()).contains("abcdef***").doesNotContain(TOKEN);
    }
}
