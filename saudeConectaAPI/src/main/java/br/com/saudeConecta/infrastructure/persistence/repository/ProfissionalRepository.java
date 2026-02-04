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
    
    Page<Profissional> findByOrganizacao_Id(Long organizacaoId, Pageable pageable);
    
    Optional<Profissional> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);
    
    List<Profissional> findByOrganizacao_IdAndStatus(Long organizacaoId, StatusProfissional status);
    
    List<Profissional> findByOrganizacao_IdAndTipoProfissional_Codigo(Long organizacaoId, String codigo);
    
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
    
    @Query("SELECT p FROM Profissional p " +
           "WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Profissional> findByOrganizacaoIdAndNomeContaining(
        @Param("orgId") Long organizacaoId, 
        @Param("nome") String nome);
    
    @Query("SELECT COUNT(p) FROM Profissional p " +
           "WHERE p.organizacao.id = :orgId AND p.status = 'ATIVO'")
    Long countAtivosByOrganizacaoId(@Param("orgId") Long organizacaoId);
    
    Optional<Profissional> findByUsuario_Id(Long usuarioId);
    
    Optional<Profissional> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByEmailAndOrganizacao_Id(String email, Long organizacaoId);
}
