package br.com.saudeConecta.domain.planos;

/**
 * Identifica quem originou o cancelamento de uma assinatura.
 */
public enum OrigemCancelamento {

    CLIENTE("Cliente"),
    SUPER_ADMIN("Super Admin"),
    MERCADO_PAGO("Mercado Pago"),
    SISTEMA("Sistema");

    private final String descricao;

    OrigemCancelamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
