package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    UserDetails findByLogin(String login);
    
    Usuario findUsuarioByLogin(String login);
    
    boolean existsByLogin(String login);
    
    List<Usuario> findByOrganizacao_Id(Long organizacaoId);
    
    Optional<Usuario> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);
    
    List<Usuario> findByOrganizacao_IdAndTipoUsuarioNovo(Long organizacaoId, TipoUsuarioNovo tipoUsuario);
    
    @Query("SELECT u FROM Usuario u WHERE u.organizacao.id = :orgId AND u.status = 1")
    List<Usuario> findAtivosbyOrganizacaoId(@Param("orgId") Long organizacaoId);
    
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.organizacao.id = :orgId")
    Long countByOrganizacaoId(@Param("orgId") Long organizacaoId);
    
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.organizacao WHERE u.login = :login")
    Optional<Usuario> findByLoginWithOrganizacao(@Param("login") String login);
}
