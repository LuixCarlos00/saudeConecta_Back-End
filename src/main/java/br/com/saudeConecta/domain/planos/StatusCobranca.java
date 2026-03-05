package br.com.saudeConecta.domain.planos;

public enum StatusCobranca {

    PENDENTE("Pendente"),
    PAGO("Pago"),
    EXPIRADO("Expirado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusCobranca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
