package br.com.saudeConecta.infrastructure.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta da API de assinaturas do Mercado Pago (/preapproval).
 *
 * @param id                identificador do preapproval
 * @param status            status no Mercado Pago (pending, authorized, paused, cancelled)
 * @param initPoint         URL do checkout hospedado onde o cliente informa o cartão
 * @param payerEmail        e-mail do pagador vinculado à assinatura
 * @param externalReference referência local (ASSINATURA-{id})
 * @param autoRecurring     dados de recorrência
 * @param nextPaymentDate   data/hora (ISO-8601 com offset) da próxima cobrança agendada
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreapprovalResponse(
        String id,
        String status,
        @JsonProperty("init_point") String initPoint,
        @JsonProperty("payer_email") String payerEmail,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("auto_recurring") AutoRecurring autoRecurring,
        @JsonProperty("next_payment_date") String nextPaymentDate
) {

    /**
     * Data da próxima cobrança agendada no Mercado Pago.
     * Usa só a parte da data, sem converter fuso (o tratamento de fuso fica para a Fase 5).
     *
     * @return a data, ou null quando ausente ou em formato inesperado
     */
    public LocalDate proximaCobranca() {
        if (nextPaymentDate == null || nextPaymentDate.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(nextPaymentDate.substring(0, 10));
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AutoRecurring(
            Integer frequency,
            @JsonProperty("frequency_type") String frequencyType,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount,
            @JsonProperty("currency_id") String currencyId
    ) {}
}
