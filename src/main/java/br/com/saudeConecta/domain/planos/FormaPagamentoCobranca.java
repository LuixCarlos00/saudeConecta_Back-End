package br.com.saudeConecta.domain.planos;

/**
 * Forma de pagamento usada na cobrança do tenant.
 * PIX existe apenas para leitura do histórico anterior à migração.
 */
public enum FormaPagamentoCobranca {

    MERCADO_PAGO("Mercado Pago"),
    PIX("Pix"),
    MANUAL("Manual");

    private final String descricao;

    FormaPagamentoCobranca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
