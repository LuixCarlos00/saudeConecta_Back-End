package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
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

    /**
     * Busca usuário por ID carregando Organização em JOIN FETCH.
     * Evita lazy load de organizacao no buscarPerfilCompleto, reduzindo de 2 queries para 1.
     *
     * @param id ID do usuário
     * @return Optional com Usuario e Organizacao já carregados
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.organizacao WHERE u.id = :id")
    Optional<Usuario> findByIdWithOrganizacao(@Param("id") Long id);

    /**
     * Busca usuário pelo email na tabela usuarios, restrito a usuários sem organização (ex: SUPER_ADMIN).
     * Utilizado na recuperação de senha para usuários master que não possuem perfil em outras tabelas.
     *
     * @param email Email cadastrado na coluna email da tabela usuarios
     * @return Optional com o Usuario encontrado
     */
    Optional<Usuario> findByEmailAndOrganizacaoIsNull(String email);

    /**
     * Busca um único usuário pelo tipo e sem organização vinculada.
     * Utilizado para localizar o SUPER_ADMIN na autenticação via senha master.
     *
     * @param tipoUsuarioNovo Tipo do usuário (ex: SUPER_ADMIN)
     * @return Usuario encontrado ou null
     */
    Usuario findByTipoUsuarioNovoAndOrganizacaoIsNull(TipoUsuarioNovo tipoUsuarioNovo);

}
