package br.com.saudeConecta.domain.planos;

public enum TipoPlano {

    STARTER("Starter"),
    PROFISSIONAL("Profissional"),
    BUSINESS("Business");

    private final String descricao;

    TipoPlano(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
