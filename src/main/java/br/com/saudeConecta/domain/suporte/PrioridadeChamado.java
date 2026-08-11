package br.com.saudeConecta.domain.suporte;

/**
 * Prioridade do chamado de suporte.
 * Cada prioridade define o SLA (prazo previsto de atendimento) em dias uteis.
 */
public enum PrioridadeChamado {

    BAIXA("Baixa", 5),
    MEDIA("Media", 3),
    ALTA("Alta", 1);

    private final String descricao;
    private final int prazoPrevistoDias;

    PrioridadeChamado(String descricao, int prazoPrevistoDias) {
        this.descricao = descricao;
        this.prazoPrevistoDias = prazoPrevistoDias;
    }

    /**
     * @return descricao amigavel da prioridade
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * @return prazo previsto de atendimento em dias
     */
    public int getPrazoPrevistoDias() {
        return prazoPrevistoDias;
    }
}
