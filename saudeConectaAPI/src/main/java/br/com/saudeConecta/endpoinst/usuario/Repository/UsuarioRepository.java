package br.com.saudeConecta.endpoinst.usuario.Repository;

import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    UserDetails findBylogin(String username);

    boolean existsByLogin(String login);


}
