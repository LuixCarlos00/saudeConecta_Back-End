package br.com.saudeConecta.domain.usuario;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public enum TipoUsuarioNovo {

    ROOT(0, "Super Administrador", List.of(
        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    )),

    GESTOR(1, "Gestor", List.of(
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    )),

    CLINICO(2, "Clínico", List.of(
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL")
    )),

    ASSISTENTE(3, "Assistente", List.of(
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    ));

    private final int codigo;
    private final String descricao;
    private final List<SimpleGrantedAuthority> authorities;

    TipoUsuarioNovo(int codigo, String descricao, List<SimpleGrantedAuthority> authorities) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.authorities = authorities;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }

    public static TipoUsuarioNovo fromCodigo(int codigo) {
        for (TipoUsuarioNovo tipo : values()) {
            if (tipo.codigo == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de tipo de usuário inválido: " + codigo);
    }

    public boolean isRoot() {
        return this == ROOT;
    }

    public boolean isGestor() {
        return this == GESTOR;
    }

    public boolean isClinico() {
        return this == CLINICO;
    }

    public boolean isAssistente() {
        return this == ASSISTENTE;
    }

    public boolean canManageOrganization() {
        return this == ROOT || this == GESTOR;
    }
}
