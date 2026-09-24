package br.com.saudeConecta.service.mercadopago;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MercadoPagoSignatureValidator - Testes unitários")
class MercadoPagoSignatureValidatorTest {

    private static final String SECRET = "segredo-de-teste";
    private static final String DATA_ID = "123456789";
    private static final String REQUEST_ID = "req-abc-123";

    private MercadoPagoProperties properties;
    private MercadoPagoSignatureValidator validator;

    @BeforeEach
    void setUp() {
        properties = new MercadoPagoProperties();
        properties.setWebhookSecret(SECRET);
        validator = new MercadoPagoSignatureValidator(properties);
    }

    private String assinar(String dataId, String requestId, long ts) throws Exception {
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return "ts=" + ts + ",v1=" + hex;
    }

    @Test
    @DisplayName("Deve aceitar assinatura válida dentro da janela de tolerância")
    void deveAceitarAssinaturaValida() throws Exception {
        long ts = Instant.now().getEpochSecond();
        String header = assinar(DATA_ID, REQUEST_ID, ts);

        assertThat(validator.isValida(header, REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar assinatura calculada com outro data.id")
    void deveRejeitarAssinaturaDeOutroRecurso() throws Exception {
        long ts = Instant.now().getEpochSecond();
        String header = assinar("outro-id", REQUEST_ID, ts);

        assertThat(validator.isValida(header, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar assinatura ausente")
    void deveRejeitarAssinaturaAusente() {
        assertThat(validator.isValida(null, REQUEST_ID, DATA_ID)).isFalse();
        assertThat(validator.isValida("   ", REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar header malformado")
    void deveRejeitarHeaderMalformado() {
        assertThat(validator.isValida("assinatura-invalida", REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar timestamp fora da janela de tolerância (replay attack)")
    void deveRejeitarTimestampAntigo() throws Exception {
        long ts = Instant.now().getEpochSecond() - 3600;
        String header = assinar(DATA_ID, REQUEST_ID, ts);

        assertThat(validator.isValida(header, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar quando o secret não está configurado")
    void deveRejeitarQuandoSecretAusente() throws Exception {
        long ts = Instant.now().getEpochSecond();
        String header = assinar(DATA_ID, REQUEST_ID, ts);
        properties.setWebhookSecret(null);

        assertThat(validator.isValida(header, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar quando data.id não é informado")
    void deveRejeitarSemDataId() throws Exception {
        long ts = Instant.now().getEpochSecond();
        String header = assinar(DATA_ID, REQUEST_ID, ts);

        assertThat(validator.isValida(header, REQUEST_ID, null)).isFalse();
    }
}
