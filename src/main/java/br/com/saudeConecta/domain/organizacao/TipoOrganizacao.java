package br.com.saudeConecta.domain.organizacao;

public enum TipoOrganizacao {
    CLINICA_MEDICA("Clínica Médica"),
    CLINICA_ODONTOLOGICA("Clínica Odontológica"),
    MISTA("Clínica Mista"),
    CLINICA("Clínica"),
    CONSULTORIO("Consultório"),
    HOSPITAL("Hospital"),
    LABORATORIO("Laboratório"),
    UPA("UPA");
    
    private final String descricao;
    
    TipoOrganizacao(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
