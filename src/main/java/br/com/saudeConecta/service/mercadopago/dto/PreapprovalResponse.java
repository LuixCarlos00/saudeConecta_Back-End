package br.com.saudeConecta.service.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Resposta da API de assinaturas do Mercado Pago (/preapproval).
 *
 * @param id                identificador do preapproval
 * @param status            status no Mercado Pago (pending, authorized, paused, cancelled)
 * @param initPoint         URL do checkout hospedado onde o cliente informa o cartão
 * @param payerEmail        e-mail do pagador vinculado à assinatura
 * @param externalReference referência local (ASSINATURA-{id})
 * @param autoRecurring     dados de recorrência
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreapprovalResponse(
        String id,
        String status,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("payer_email") String payerEmail,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("auto_recurring") AutoRecurring autoRecurring
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AutoRecurring(
            Integer frequency,
            @JsonProperty("frequency_type") String frequencyType,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("currency_id") String currencyId
    ) {}
}
