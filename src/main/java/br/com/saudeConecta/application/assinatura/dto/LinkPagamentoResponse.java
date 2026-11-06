package br.com.saudeConecta.application.assinatura.dto;

/**
 * Link do checkout hospedado do Mercado Pago para ativação da assinatura.
 *
 * @param preapprovalId identificador da assinatura no Mercado Pago
 * @param initPoint     URL do checkout onde o cliente cadastra o cartão
 * @param payerEmail    e-mail que precisa estar vinculado à conta Mercado Pago do pagador
 * @param valorMensal   valor que será cobrado mensalmente
 */
public record LinkPagamentoResponse(
        String preapprovalId,
        String initPoint,
        String payerEmail,
        java.math.BigDecimal valorMensal
) {}
