package br.com.saudeConecta.endpoinst.usuario.Entity;

import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.usuario.DTO.DadosLoginUsuario;
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
public class Usuario implements Serializable , UserDetails {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id  ;

    @Column(nullable = false, name = "login")
    private String login;

    @Column(nullable = false, name = "senha")
    private String senha  ;

    @Column(nullable = false, name = "TipoUsuario")
    private Byte TipoUsuario ;


    public Usuario(String login, String senha ) {

        this.login =login;
        this.senha = senha;

    }

    public Usuario(DadosLoginUsuario dados, String senhaCriptografada) {
        this.login=dados.login();
        this.senha=senhaCriptografada;
    }

    public Usuario(String login) {
        this.login =getLogin();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.TipoUsuario == 1) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        else if (this.TipoUsuario == 2) {
            return List.of(new SimpleGrantedAuthority("ROLE_Secretaria"));
        }
        else if (this.TipoUsuario == 3) {
            return List.of(new SimpleGrantedAuthority("ROLE_Medico"));
        }
        else   return List.of(new SimpleGrantedAuthority("ROLE_USER"));
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
        return true;
    }

    public void update(Usuario dados) {
        this.login = dados.getLogin();
        this.senha= dados.getSenha();


    }
}
