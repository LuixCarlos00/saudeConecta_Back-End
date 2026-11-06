package br.com.saudeConecta.presentation.controller.assinatura;

import br.com.saudeConecta.application.assinatura.MercadoPagoSignatureValidator;
import br.com.saudeConecta.application.assinatura.MercadoPagoWebhookService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercadoPagoWebhookController - injeção de log")
class MercadoPagoWebhookControllerTest {

    @Mock private MercadoPagoSignatureValidator signatureValidator;
    @Mock private MercadoPagoWebhookService webhookService;
    @InjectMocks private MercadoPagoWebhookController controller;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void anexarAppender() {
        logger = (Logger) LoggerFactory.getLogger(MercadoPagoWebhookController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void removerAppender() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("dataId e tipo com CR/LF vão sanitizados (e truncados) ao log e crus ao processamento")
    void deveSanitizarDataIdNoLogAceito() {
        String dataIdMalicioso = "123\r\nmp.webhook aceito tipo=FORJADO";
        when(signatureValidator.isValida(any(), any(), any())).thenReturn(true);

        ResponseEntity<Void> resposta = controller.receber("sig", "req", "subscription\n_preapproval",
                dataIdMalicioso, null);

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(appender.list).hasSize(1);
        String mensagem = appender.list.get(0).getFormattedMessage();
        assertThat(mensagem).doesNotContain("\r").doesNotContain("\n").isEqualTo(
                "mp.webhook aceito tipo=subscription__preapproval dataId=123__mp.webhook_aceito_tipo_FORJ");
        verify(webhookService).processarAssincrono("subscription\n_preapproval", dataIdMalicioso, null);
    }

    @Test
    @DisplayName("assinatura inválida com tipo contendo CR/LF não quebra a linha de log")
    void deveSanitizarTipoNoLogDeNegado() {
        when(signatureValidator.isValida(any(), any(), any())).thenReturn(false);

        ResponseEntity<Void> resposta = controller.receber("sig", "req", "x\r\ny", "1", null);

        assertThat(resposta.getStatusCode().value()).isEqualTo(401);
        assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("mp.webhook negado motivo=ASSINATURA_INVALIDA tipo=x__y");
    }
}
