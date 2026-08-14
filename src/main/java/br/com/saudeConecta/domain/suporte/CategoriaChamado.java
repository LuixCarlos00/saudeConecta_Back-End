package br.com.saudeConecta.domain.suporte;

/**
 * Categoria do chamado de suporte aberto pelo usuario.
 */
public enum CategoriaChamado {

    DUVIDA_TECNICA("Duvida tecnica"),
    PROBLEMA_SISTEMA("Problema no sistema"),
    SUGESTAO_MELHORIA("Sugestao de melhoria"),
    SOLICITACAO_FUNCIONALIDADE("Solicitacao de funcionalidade"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaChamado(String descricao) {
        this.descricao = descricao;
    }

    /**
     * @return descricao amigavel da categoria para exibicao e emails
     */
    public String getDescricao() {
        return descricao;
    }
}
