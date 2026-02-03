package br.com.saudeConecta.application.port.in.usuario;

import br.com.saudeConecta.domain.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface UsuarioInputPort {
    
    Optional<Usuario> buscarPorId(Long id);
    
    Optional<Usuario> buscarPorLogin(String login);
    
    UserDetails buscarUserDetailsPorLogin(String login);
    
    List<Usuario> buscarTodos();
    
    Page<Usuario> buscarTodos(Pageable pageable);
    
    boolean existePorLogin(String login);
    
    Usuario cadastrar(Usuario usuario);
    
    void deletar(Long id) throws Exception;
}
