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










    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.usuario LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :organizacaoId")
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
           "LEFT JOIN FETCH p.endereco " +
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
    

    
    @Query("SELECT COUNT(p) FROM Profissional p " +
           "WHERE p.organizacao.id = :orgId AND p.status IN ('ATIVO', 'INATIVO')")
    Long countAtivosByOrganizacaoId(@Param("orgId") Long organizacaoId);

    @Query("SELECT COUNT(p) FROM Profissional p WHERE p.status = 'ATIVO'")
    Long countTodosAtivos();
    
    Optional<Profissional> findByUsuario_Id(Long usuarioId);

    @Query("SELECT p FROM Profissional p WHERE p.usuario.id = :usuarioId AND p.organizacao.id = :organizacaoId")
    Optional<Profissional> findByUsuarioAndOrganizacao_Id(@Param("usuarioId") Long usuarioId, @Param("organizacaoId") Long organizacaoId);

    Optional<Profissional> findByEmail(String email);


    @Query("SELECT p FROM Profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "LEFT JOIN FETCH p.endereco " +
           "LEFT JOIN FETCH p.usuario " +
           "WHERE p.usuario.id = :usuarioId")
    Optional<Profissional> findByUsuarioIdWithRelations(@Param("usuarioId") Long usuarioId);
    

    

    boolean existsByCpfAndOrganizacao_Id(String cpf, Long organizacaoId);
    



    @Query("SELECT COUNT(p) FROM Profissional p WHERE p.status = 'ATIVO'")
    Long countAllAtivos();

    List<Profissional> findByStatus(StatusProfissional status);
    
    // ========== MÉTODOS DE BUSCA PARA AUTOCOMPLETE ==========
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Profissional> findByOrganizacaoIdAndNomeContaining(@Param("orgId") Long orgId, @Param("nome") String nome);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId AND LOWER(p.registroConselho) LIKE LOWER(CONCAT('%', :crm, '%'))")
    List<Profissional> findByOrganizacaoIdAndCrmContaining(@Param("orgId") Long orgId, @Param("crm") String crm);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId AND LOWER(p.endereco.endMunicipio) LIKE LOWER(CONCAT('%', :cidade, '%'))")
    List<Profissional> findByOrganizacaoIdAndCidadeContaining(@Param("orgId") Long orgId, @Param("cidade") String cidade);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId AND EXISTS (SELECT 1 FROM p.especialidades e WHERE LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')))")
    List<Profissional> findByOrganizacaoIdAndEspecialidadeContaining(@Param("orgId") Long orgId, @Param("especialidade") String especialidade);
    
    // ========== MÉTODOS COM FILTRO DE STATUS ==========
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco LEFT JOIN FETCH p.usuario WHERE p.organizacao.id = :orgId " +
           "AND (:filtro = 'ALL' OR p.status = :status)")
    List<Profissional> findByOrganizacao_IdWithUsuarioWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("filtro") String filtro,
        @Param("status") StatusProfissional status);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%')) " +
           "AND (:filtro = 'ALL' OR p.status = :status)")
    List<Profissional> findByOrganizacaoIdAndNomeContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("nome") String nome,
        @Param("filtro") String filtro,
        @Param("status") StatusProfissional status);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.registroConselho) LIKE LOWER(CONCAT('%', :crm, '%')) " +
           "AND (:filtro = 'ALL' OR p.status = :status)")
    List<Profissional> findByOrganizacaoIdAndCrmContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("crm") String crm,
        @Param("filtro") String filtro,
        @Param("status") StatusProfissional status);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.endereco.endMunicipio) LIKE LOWER(CONCAT('%', :cidade, '%')) " +
           "AND (:filtro = 'ALL' OR p.status = :status)")
    List<Profissional> findByOrganizacaoIdAndCidadeContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("cidade") String cidade,
        @Param("filtro") String filtro,
        @Param("status") StatusProfissional status);
    
    @Query("SELECT p FROM Profissional p LEFT JOIN FETCH p.tipoProfissional LEFT JOIN FETCH p.especialidades LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND EXISTS (SELECT 1 FROM p.especialidades e WHERE LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%'))) " +
           "AND (:filtro = 'ALL' OR p.status = :status)")
    List<Profissional> findByOrganizacaoIdAndEspecialidadeContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("especialidade") String especialidade,
        @Param("filtro") String filtro,
        @Param("status") StatusProfissional status);
}
