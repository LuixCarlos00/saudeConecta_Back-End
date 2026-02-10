package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    
    List<Profissional> findByOrganizacao_Id(Long organizacaoId);

    Optional<Profissional> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);










    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.usuario WHERE p.organizacao.id = :organizacaoId")
    List<Profissional> findByOrganizacao_IdWithUsuario(@Param("organizacaoId") Long organizacaoId);
    
    Page<Profissional> findByOrganizacao_Id(Long organizacaoId, Pageable pageable);
    
    @Query("SELECT p FROM Profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "LEFT JOIN FETCH p.endereco " +
           "WHERE p.id = :id AND p.organizacao.id = :organizacaoId")
    Optional<Profissional> buscarClinicoIdByOrg(@Param("id") Long id, @Param("organizacaoId") Long organizacaoId);
    


    @Query("SELECT DISTINCT p FROM Profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "WHERE p.organizacao.id = :organizacaoId AND p.status = :status")
    List<Profissional> findByOrganizacaoIdAndStatusWithRelations(
        @Param("organizacaoId") Long organizacaoId,
        @Param("status") StatusProfissional status);
    

    
    @Query("SELECT p FROM Profissional p WHERE p.organizacao.id = :orgId " +
           "AND p.tipoProfissional.codigo = :tipo AND p.status = 'ATIVO'")
    List<Profissional> findAtivosByOrganizacaoIdAndTipo(
        @Param("orgId") Long organizacaoId, 
        @Param("tipo") String tipoCodigo);
    
    @Query("SELECT p FROM Profissional p " +
           "LEFT JOIN FETCH p.especialidades " +
           "WHERE p.organizacao.id = :orgId AND p.id = :id")
    Optional<Profissional> findByIdWithEspecialidades(
        @Param("orgId") Long organizacaoId, 
        @Param("id") Long id);
    
//    @Query("SELECT p FROM Profissional p " +
//           "WHERE p.organizacao.id = :orgId " +
//           "AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
//    List<Profissional> findByOrganizacaoIdAndNomeContaining(
//        @Param("orgId") Long organizacaoId,
//        @Param("nome") String nome);
    
    @Query("SELECT COUNT(p) FROM Profissional p " +
           "WHERE p.organizacao.id = :orgId AND p.status = 'ATIVO'")
    Long countAtivosByOrganizacaoId(@Param("orgId") Long organizacaoId);
    
    Optional<Profissional> findByUsuario_Id(Long usuarioId);

    @Query("SELECT p FROM Profissional p WHERE p.usuario.id = :usuarioId AND p.organizacao.id = :organizacaoId")
    Optional<Profissional> findByUsuarioAndOrganizacao_Id(@Param("usuarioId") Long usuarioId, @Param("organizacaoId") Long organizacaoId);



    @Query("SELECT p FROM Profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "LEFT JOIN FETCH p.endereco " +
           "LEFT JOIN FETCH p.usuario " +
           "WHERE p.usuario.id = :usuarioId")
    Optional<Profissional> findByUsuarioIdWithRelations(@Param("usuarioId") Long usuarioId);
    

    
    boolean existsByCpf(String cpf);
    
    boolean existsByCpfAndOrganizacao_Id(String cpf, Long organizacaoId);
    
    boolean existsByEmailAndOrganizacao_Id(String email, Long organizacaoId);

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    @Query("SELECT COUNT(p) FROM Profissional p WHERE p.status = 'ATIVO'")
    Long countAllAtivos();

    List<Profissional> findByStatus(StatusProfissional status);
}
