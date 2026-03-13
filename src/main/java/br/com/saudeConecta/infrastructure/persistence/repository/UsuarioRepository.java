package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    UserDetails findByLogin(String login);
    
    Usuario findUsuarioByLogin(String login);
    
    boolean existsByLogin(String login);
    
    List<Usuario> findByOrganizacao_Id(Long organizacaoId);


}
