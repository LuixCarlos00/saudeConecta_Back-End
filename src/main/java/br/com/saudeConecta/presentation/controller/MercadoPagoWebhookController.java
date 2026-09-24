package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.mercadopago.MercadoPagoSignatureValidator;
import br.com.saudeConecta.service.mercadopago.MercadoPagoWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recebe as notificações de assinatura do Mercado Pago.
 *
 * O Mercado Pago exige resposta em até 22 segundos e reenvia a notificação
 * enquanto não receber 2xx — por isso o endpoint responde imediatamente e
 * delega o processamento (idempotente) para uma thread assíncrona.
 */
@RestController
@RequestMapping("/publico/webhooks/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final MercadoPagoSignatureValidator signatureValidator;
    private final MercadoPagoWebhookService webhookService;

    /**
     * Endpoint de notificações. Valida a assinatura HMAC antes de aceitar o evento.
     *
     * @param xSignature header x-signature enviado pelo Mercado Pago
     * @param xRequestId header x-request-id enviado pelo Mercado Pago
     * @param tipoQuery  tópico informado via query string (formato antigo)
     * @param idQuery    id informado via query string (formato antigo)
     * @param corpo      corpo JSON da notificação
     * @return 200 quando o evento é aceito, 401 quando a assinatura é inválida
     */
    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "type", required = false) String tipoQuery,
            @RequestParam(value = "data.id", required = false) String idQuery,
            @RequestBody(required = false) JsonNode corpo) {

        String tipo = primeiroNaoVazio(tipoQuery, texto(corpo, "type"), texto(corpo, "topic"));
        String dataId = primeiroNaoVazio(idQuery, dataId(corpo), texto(corpo, "id"));

        if (!signatureValidator.isValida(xSignature, xRequestId, dataId)) {
            log.warn("Webhook do Mercado Pago rejeitado por assinatura inválida (tipo={})", tipo);
            return ResponseEntity.status(401).build();
        }

        if (tipo == null || dataId == null) {
            log.warn("Webhook do Mercado Pago sem tipo ou data.id — ignorado");
            return ResponseEntity.ok().build();
        }

        log.info("Webhook Mercado Pago recebido: tipo={}, data.id={}", tipo, dataId);
        webhookService.processarAssincrono(tipo, dataId, corpo != null ? corpo.toString() : null);

        return ResponseEntity.ok().build();
    }

    private String dataId(JsonNode corpo) {
        if (corpo == null || !corpo.hasNonNull("data")) {
            return null;
        }
        JsonNode id = corpo.get("data").get("id");
        return id != null && !id.isNull() ? id.asText() : null;
    }

    private String texto(JsonNode corpo, String campo) {
        if (corpo == null || !corpo.hasNonNull(campo)) {
            return null;
        }
        return corpo.get(campo).asText();
    }

    private String primeiroNaoVazio(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }
}
