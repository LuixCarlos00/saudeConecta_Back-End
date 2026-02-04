package br.com.saudeConecta.application.port.out.usuario;

import br.com.saudeConecta.domain.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface UsuarioOutputPort {
    
    Optional<Usuario> findById(Long id);
    
    Optional<Usuario> findUsuarioByLogin(String login);
    
    UserDetails findByLogin(String login);
    
    List<Usuario> findAll();
    
    Page<Usuario> findAll(Pageable pageable);
    
    boolean existsByLogin(String login);
    
    Usuario save(Usuario usuario);
    
    void deleteById(Long id);
    
    boolean existsById(Long id);
}
