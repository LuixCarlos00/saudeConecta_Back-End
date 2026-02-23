package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminOrganizacaoRepository extends JpaRepository<AdminOrganizacao, Long> {
    
    Optional<AdminOrganizacao> findByUsuario_Id(Long usuarioId);

    @Query("SELECT a FROM AdminOrganizacao a WHERE a.usuario.id = :usuarioId AND a.organizacao.id = :organizacaoId")
    Optional<AdminOrganizacao> findByUsuarioAndOrganizacao_Id(@Param("usuarioId") Long usuarioId, @Param("organizacaoId") Long organizacaoId);
    
    List<AdminOrganizacao> findByOrganizacao_Id(Long organizacaoId);
    
    @Query("SELECT a FROM AdminOrganizacao a LEFT JOIN FETCH a.usuario WHERE a.organizacao.id = :organizacaoId")
    List<AdminOrganizacao> findByOrganizacao_IdWithUsuario(@Param("organizacaoId") Long organizacaoId);
    
    @Query("SELECT a FROM AdminOrganizacao a " +
           "LEFT JOIN FETCH a.usuario " +
           "LEFT JOIN FETCH a.organizacao " +
           "WHERE a.usuario.id = :usuarioId")
    Optional<AdminOrganizacao> findByUsuarioIdWithRelations(@Param("usuarioId") Long usuarioId);

    Optional<AdminOrganizacao> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    Optional<AdminOrganizacao> findByEmail(String email);

    @Query("SELECT a FROM AdminOrganizacao a LEFT JOIN FETCH a.usuario LEFT JOIN FETCH a.organizacao")
    List<AdminOrganizacao> findAllWithRelations();

}
