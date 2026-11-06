package br.com.saudeConecta.application.assinatura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.infrastructure.mercadopago.MercadoPagoProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Valida a assinatura HMAC-SHA256 enviada pelo Mercado Pago no header {@code x-signature}.
 *
 * Formato do header: {@code ts=1704908010,v1=618c8534...}
 * Manifest assinado: {@code id:{data.id};request-id:{x-request-id};ts:{ts};}
 */
@Component
public class MercadoPagoSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoSignatureValidator.class);

    private static final String ALGORITMO = "HmacSHA256";
    private static final long JANELA_TOLERANCIA_SEGUNDOS = 300;

    private final MercadoPagoProperties properties;

    public MercadoPagoSignatureValidator(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    /**
     * Verifica se a notificação foi realmente emitida pelo Mercado Pago.
     *
     * @param xSignature conteúdo do header x-signature
     * @param xRequestId conteúdo do header x-request-id
     * @param dataId     identificador do recurso notificado (data.id)
     * @return true se a assinatura for válida e estiver dentro da janela de tolerância
     */
    public boolean isValida(String xSignature, String xRequestId, String dataId) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.error("mp.webhook.assinatura negado motivo=SECRET_NAO_CONFIGURADO");
            return false;
        }
        if (xSignature == null || xSignature.isBlank() || dataId == null || dataId.isBlank()) {
            log.warn("mp.webhook.assinatura negado motivo=SEM_ASSINATURA_OU_DATA_ID");
            return false;
        }

        String ts = extrairParte(xSignature, "ts");
        String v1 = extrairParte(xSignature, "v1");
        if (ts == null || v1 == null) {
            // Só o tamanho: o header carrega o HMAC (v1), que não deve ir para o log
            log.warn("mp.webhook.assinatura negado motivo=HEADER_MALFORMADO tamanho={} tsPresente={} v1Presente={}",
                    xSignature.length(), ts != null, v1 != null);
            return false;
        }
        if (!isTimestampValido(ts)) {
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + (xRequestId == null ? "" : xRequestId) + ";ts:" + ts + ";";
        String esperado = calcularHmac(manifest, secret);

        boolean valida = esperado != null && MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));

        if (!valida && log.isWarnEnabled()) {
            log.warn("mp.webhook.assinatura negado motivo=ASSINATURA_INVALIDA dataId={}", MascaraLog.seguro(dataId));
        }
        return valida;
    }

    /**
     * Rejeita notificações antigas para mitigar replay attacks.
     */
    private boolean isTimestampValido(String ts) {
        try {
            long diferenca = Math.abs(Instant.now().getEpochSecond() - Long.parseLong(ts));
            if (diferenca > JANELA_TOLERANCIA_SEGUNDOS) {
                log.warn("mp.webhook.assinatura negado motivo=FORA_DA_JANELA diferencaSeg={}", diferenca);
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            log.warn("mp.webhook.assinatura negado motivo=TIMESTAMP_INVALIDO ts={}", MascaraLog.seguro(ts));
            return false;
        }
    }

    private String extrairParte(String xSignature, String chave) {
        for (String parte : xSignature.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length == 2 && chave.equals(par[0].trim())) {
                return par[1].trim();
            }
        }
        return null;
    }

    private String calcularHmac(String manifest, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            log.error("mp.webhook.assinatura falha motivo=ERRO_HMAC", e);
            return null;
        }
    }
}
