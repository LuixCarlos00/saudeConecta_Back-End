package br.com.saudeConecta.domain.suporte;

/**
 * Status do ciclo de vida de um chamado de suporte.
 */
public enum StatusChamado {

    EM_ANALISE("Em analise"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDO("Concluido"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusChamado(String descricao) {
        this.descricao = descricao;
    }

    /**
     * @return descricao amigavel do status
     */
    public String getDescricao() {
        return descricao;
    }
}
