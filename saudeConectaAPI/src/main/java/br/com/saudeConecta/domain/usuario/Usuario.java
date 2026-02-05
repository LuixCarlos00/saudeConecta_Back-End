package br.com.saudeConecta.domain.usuario;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.infra.tenant.TenantAware;
import br.com.saudeConecta.presentation.dto.usuario.CadastrarUsuarioRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(nullable = false, name = "login")
    private String login;

    @Column(nullable = false, name = "senha")
    private String senha;

    @Column(nullable = false, name = "TipoUsuario")
    private Byte tipoUsuario;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario_novo")
    private TipoUsuarioNovo tipoUsuarioNovo;

    @Column(nullable = false, name = "status")
    private Byte status;

    public Usuario(CadastrarUsuarioRequest dados, String senhaCriptografada) {
        this.login = dados.login();
        this.senha = senhaCriptografada;
        this.tipoUsuario = dados.tipoUsuario();
        this.status = dados.status();
    }
    
    public Usuario(CadastrarUsuarioRequest dados, String senhaCriptografada, 
                   Organizacao organizacao, TipoUsuarioNovo tipoNovo) {
        this.login = dados.login();
        this.senha = senhaCriptografada;
        this.tipoUsuario = dados.tipoUsuario();
        this.tipoUsuarioNovo = tipoNovo;
        this.status = dados.status();
        this.organizacao = organizacao;
    }


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
        return this.status != null && this.status == 1;
    }

    public void update(Usuario dados) {
        this.login = dados.getLogin();
        this.senha = dados.getSenha();
    }
    
    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }
    
    @Override
    public void setOrganizacaoId(Long organizacaoId) {
    }
    
    public boolean isSuperAdmin() {
        return TipoUsuarioNovo.SUPER_ADMIN.equals(this.tipoUsuarioNovo);
    }
    
    public boolean isAdminOrganizacao() {
        return TipoUsuarioNovo.ADMIN_ORG.equals(this.tipoUsuarioNovo);
    }
    
    public boolean isProfissional() {
        return TipoUsuarioNovo.PROFISSIONAL.equals(this.tipoUsuarioNovo);
    }
    
    public boolean hasOrganization() {
        return this.organizacao != null;
    }
}
