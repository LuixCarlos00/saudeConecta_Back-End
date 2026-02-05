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
    
    List<AdminOrganizacao> findByOrganizacao_Id(Long organizacaoId);
    
    @Query("SELECT a FROM AdminOrganizacao a " +
           "LEFT JOIN FETCH a.usuario " +
           "LEFT JOIN FETCH a.organizacao " +
           "WHERE a.usuario.id = :usuarioId")
    Optional<AdminOrganizacao> findByUsuarioIdWithRelations(@Param("usuarioId") Long usuarioId);
    
    boolean existsByOrganizacao_IdAndUsuario_Id(Long organizacaoId, Long usuarioId);
}
