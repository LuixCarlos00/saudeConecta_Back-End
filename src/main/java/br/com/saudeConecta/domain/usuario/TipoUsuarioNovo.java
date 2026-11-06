package br.com.saudeConecta.domain.usuario;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public enum TipoUsuarioNovo {

    ROOT(0, "Super Administrador", List.of(
        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority(Papel.PROFISSIONAL),
        new SimpleGrantedAuthority(Papel.RECEPCIONISTA)
    )),

    GESTOR(1, "Gestor", List.of(
        new SimpleGrantedAuthority("ROLE_ADMIN"),
        new SimpleGrantedAuthority(Papel.PROFISSIONAL),
        new SimpleGrantedAuthority(Papel.RECEPCIONISTA)
    )),

    CLINICO(2, "Clínico", List.of(
        new SimpleGrantedAuthority(Papel.PROFISSIONAL)
    )),

    ASSISTENTE(3, "Assistente", List.of(
        new SimpleGrantedAuthority(Papel.RECEPCIONISTA)
    ));

    /** Nomes dos papéis em classe aninhada: constantes do enum não podem referenciar campos estáticos declarados depois delas. */
    private static final class Papel {
        static final String PROFISSIONAL = "ROLE_PROFISSIONAL";
        static final String RECEPCIONISTA = "ROLE_RECEPCIONISTA";

        private Papel() {
            // Apenas agrupa constantes
        }
    }

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
}
