package br.com.saudeConecta.domain.planos;

public enum StatusAssinatura {

    TRIAL("Trial"),
    ATIVA("Ativa"),
    INADIMPLENTE("Inadimplente"),
    SUSPENSA("Suspensa"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusAssinatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAtiva() {
        return this == ATIVA || this == TRIAL;
    }

    public boolean permiteAcesso() {
        return this == ATIVA || this == TRIAL || this == INADIMPLENTE;
    }
}
