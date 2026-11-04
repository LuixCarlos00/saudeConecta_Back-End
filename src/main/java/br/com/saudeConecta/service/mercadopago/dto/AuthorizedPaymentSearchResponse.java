package br.com.saudeConecta.service.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Resposta da busca de faturas de uma assinatura no Mercado Pago
 * ({@code GET /authorized_payments/search?preapproval_id=...}).
 *
 * @param results faturas encontradas (pode vir nulo quando não há nenhuma)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizedPaymentSearchResponse(List<AuthorizedPaymentResponse> results) {

    /**
     * @return as faturas encontradas, nunca nulo
     */
    public List<AuthorizedPaymentResponse> faturas() {
        return results != null ? results : List.of();
    }
}
