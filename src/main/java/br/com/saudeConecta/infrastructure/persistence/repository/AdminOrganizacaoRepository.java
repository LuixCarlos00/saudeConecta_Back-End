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

    
    @Query("SELECT a FROM AdminOrganizacao a LEFT JOIN FETCH a.usuario WHERE a.organizacao.id = :organizacaoId")
    List<AdminOrganizacao> findByOrganizacao_IdWithUsuario(@Param("organizacaoId") Long organizacaoId);
    
    @Query("SELECT a FROM AdminOrganizacao a " +
           "LEFT JOIN FETCH a.usuario " +
           "LEFT JOIN FETCH a.organizacao o " +
           "LEFT JOIN FETCH o.endereco " +
           "WHERE a.usuario.id = :usuarioId")
    Optional<AdminOrganizacao> findByUsuarioIdWithRelations(@Param("usuarioId") Long usuarioId);

    Optional<AdminOrganizacao> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    Optional<AdminOrganizacao> findByEmail(String email);

    @Query("SELECT DISTINCT a FROM AdminOrganizacao a " +
           "LEFT JOIN FETCH a.usuario " +
           "LEFT JOIN FETCH a.organizacao o " +
           "LEFT JOIN FETCH o.assinaturas assin " +
           "LEFT JOIN FETCH assin.planoAssinatura")
    List<AdminOrganizacao> findAllWithRelations();

    @Query("SELECT a FROM AdminOrganizacao a " +
           "LEFT JOIN FETCH a.usuario " +
           "LEFT JOIN FETCH a.organizacao o " +
           "LEFT JOIN FETCH o.endereco " +
           "WHERE a.id = :id")
    Optional<AdminOrganizacao> findByIdWithOrgAndEndereco(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM admin_organizacao a " +
           "INNER JOIN usuarios u ON a.usuario_id = u.id " +
           "WHERE a.organizacao_id = :orgId AND u.status IN ('ATIVO', 'INATIVO')",
           nativeQuery = true)
    Long countAtivosByOrganizacaoId(@Param("orgId") Long organizacaoId);

}
