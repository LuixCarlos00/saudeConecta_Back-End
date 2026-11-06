package br.com.saudeConecta.infrastructure.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Resposta da busca de faturas de uma assinatura no Mercado Pago
 * ({@code GET /authorized_payments/search?preapproval_id=...&limit=&offset=}).
 *
 * ❓ Contrato de paginação a validar no sandbox do Mercado Pago: assume-se o formato
 * padrão das buscas do MP ({@code paging.total/limit/offset} + {@code results}).
 *
 * @param paging  dados de paginação (pode vir nulo se o MP não paginar)
 * @param results faturas encontradas (pode vir nulo quando não há nenhuma)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizedPaymentSearchResponse(Paging paging, List<AuthorizedPaymentResponse> results) {

    /**
     * @return as faturas encontradas, nunca nulo
     */
    public List<AuthorizedPaymentResponse> faturas() {
        return results != null ? results : List.of();
    }

    /**
     * Paginação da busca.
     *
     * @param total  total de faturas do preapproval
     * @param limit  tamanho da página aplicado pelo MP
     * @param offset deslocamento da página retornada
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Paging(Integer total, Integer limit, Integer offset) {}
}
