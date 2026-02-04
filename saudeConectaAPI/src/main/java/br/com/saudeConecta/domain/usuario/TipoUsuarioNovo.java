package br.com.saudeConecta.domain.usuario;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public enum TipoUsuarioNovo {
    
    SUPER_ADMIN("Super Administrador", List.of(
        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    )),
    
    ADMIN_ORG("Administrador da Organização", List.of(
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    )),
    
    GERENTE("Gerente", List.of(
        new SimpleGrantedAuthority("ROLE_GERENTE"),
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL"),
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    )),
    
    PROFISSIONAL("Profissional de Saúde", List.of(
        new SimpleGrantedAuthority("ROLE_PROFISSIONAL")
    )),
    
    RECEPCIONISTA("Recepcionista", List.of(
        new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")
    ));
    
    private final String descricao;
    private final List<SimpleGrantedAuthority> authorities;
    
    TipoUsuarioNovo(String descricao, List<SimpleGrantedAuthority> authorities) {
        this.descricao = descricao;
        this.authorities = authorities;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public List<SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }
    
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN_ORG;
    }
    
    public boolean canManageOrganization() {
        return this == SUPER_ADMIN || this == ADMIN_ORG || this == GERENTE;
    }
}
