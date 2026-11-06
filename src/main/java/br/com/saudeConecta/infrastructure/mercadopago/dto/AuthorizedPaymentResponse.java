package br.com.saudeConecta.infrastructure.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Resposta da API de pagamentos recorrentes do Mercado Pago (/authorized_payments).
 * Representa uma fatura mensal gerada automaticamente a partir do preapproval.
 *
 * @param id                identificador do pagamento autorizado
 * @param preapprovalId     assinatura que originou o pagamento
 * @param status            status da fatura (scheduled, processed, recycling, cancelled)
 * @param transactionAmount valor cobrado
 * @param payment           detalhe do pagamento efetivamente processado
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizedPaymentResponse(
        String id,
        @JsonProperty("preapproval_id") String preapprovalId,
        String status,
        @JsonProperty("transaction_amount") BigDecimal transactionAmount,
        Payment payment
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            Long id,
            String status,
            @JsonProperty("status_detail") String statusDetail
    ) {}

    /**
     * Status do pagamento subjacente, ou null quando o Mercado Pago ainda não
     * processou a fatura.
     *
     * @return status do pagamento (approved, rejected, pending, ...)
     */
    public String statusPagamento() {
        return payment != null ? payment.status() : null;
    }

    /**
     * Motivo da recusa informado pelo Mercado Pago.
     *
     * @return status_detail do pagamento ou null
     */
    public String motivoRecusa() {
        return payment != null ? payment.statusDetail() : null;
    }
}
