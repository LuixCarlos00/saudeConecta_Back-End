package br.com.saudeConecta.domain.usuario;

import br.com.saudeConecta.config.tenant.TenantAware;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.Convert;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuarios")
@JsonIgnoreProperties({"hibernateLazyInitializer", "senha", "password", "authorities"})
public class Usuario implements Serializable, UserDetails, TenantAware {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    @JsonIgnore
    private Organizacao organizacao;

    @Column(nullable = false, name = "login", unique = true)
    private String login;

    @Column(nullable = false, name = "senha")
    private String senha;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario_novo")
    private TipoUsuarioNovo tipoUsuarioNovo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "email", nullable = true)
    private String email;

    /** Origem do bloqueio atual; null quando ativo ou bloqueado antes da coluna existir. */
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_bloqueio", length = 20)
    private MotivoBloqueio motivoBloqueio;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (!isEnabled()) {
            return List.of();
        }
        return this.tipoUsuarioNovo != null 
            ? this.tipoUsuarioNovo.getAuthorities() 
            : List.of();
    }

    @Override
    public String getPassword() {
        return getSenha();
    }

    @Override
    public String getUsername() {
        return getLogin();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return StatusUsuario.ATIVO.equals(this.status);
    }

    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }

    @Override
    public void setOrganizacaoId(Long organizacaoId) {
        // Sem efeito: a organização é definida pela associação (campo "organizacao"); o setter existe só para cumprir a interface de tenant.
    }
    
    public boolean isRoot() {
        return TipoUsuarioNovo.ROOT.equals(this.tipoUsuarioNovo);
    }

    public boolean isGestor() {
        return TipoUsuarioNovo.GESTOR.equals(this.tipoUsuarioNovo);
    }

    public boolean isClinico() {
        return TipoUsuarioNovo.CLINICO.equals(this.tipoUsuarioNovo);
    }

    public boolean isAssistente() {
        return TipoUsuarioNovo.ASSISTENTE.equals(this.tipoUsuarioNovo);
    }
}
