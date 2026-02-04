package br.com.saudeConecta.domain.organizacao;

public enum TipoOrganizacao {
    CLINICA_MEDICA("Clínica Médica"),
    CLINICA_ODONTOLOGICA("Clínica Odontológica"),
    MISTA("Clínica Mista");
    
    private final String descricao;
    
    TipoOrganizacao(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
