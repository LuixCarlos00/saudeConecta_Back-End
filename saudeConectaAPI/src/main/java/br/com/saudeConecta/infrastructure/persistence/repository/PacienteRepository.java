package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.paciente.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    
    // Métodos legados (sem tenant)
    Optional<Paciente> findByPaciEmail(String email);
    List<Paciente> findByPaciCpfContainingIgnoreCase(String cpf);
    List<Paciente> findByPaciRgContainingIgnoreCase(String rg);
    List<Paciente> findByPaciTelefoneContainingIgnoreCase(String telefone);
    List<Paciente> findByPaciNomeContainingIgnoreCase(String nome);
    
    // Métodos com tenant (multi-tenant)
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId")
    List<Paciente> findByOrganizacao_Id(@Param("orgId") Long organizacaoId);
    
    Page<Paciente> findByOrganizacao_Id(Long organizacaoId, Pageable pageable);

    @Query("SELECT p FROM Paciente p " +
            "LEFT JOIN FETCH p.endereco " +
            "WHERE p.paciCodigo = :id AND p.organizacao.id = :orgId")
    Optional<Paciente> findByPaciCodigoAndOrganizacao_IdWithEndereco(
            @Param("id") Long id,
            @Param("orgId") Long orgId
    );
    
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciNome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Paciente> findByOrganizacaoIdAndNomeContaining(
        @Param("orgId") Long orgId, @Param("nome") String nome);
    
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciCpf) LIKE LOWER(CONCAT('%', :cpf, '%'))")
    List<Paciente> findByOrganizacaoIdAndCpfContaining(
        @Param("orgId") Long orgId, @Param("cpf") String cpf);
    
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciRg) LIKE LOWER(CONCAT('%', :rg, '%'))")
    List<Paciente> findByOrganizacaoIdAndRgContaining(
        @Param("orgId") Long orgId, @Param("rg") String rg);
    
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciTelefone) LIKE LOWER(CONCAT('%', :telefone, '%'))")
    List<Paciente> findByOrganizacaoIdAndTelefoneContaining(
        @Param("orgId") Long orgId, @Param("telefone") String telefone);
    
    @Query("SELECT COUNT(p) FROM Paciente p WHERE p.organizacao.id = :orgId AND p.paciStatus = :status")
    Long countByOrganizacaoIdAndStatus(@Param("orgId") Long orgId, @Param("status") String status);
    
    Optional<Paciente> findByPaciCpfAndOrganizacao_Id(String cpf, Long orgId);
    
    boolean existsByPaciCpfAndOrganizacao_Id(String cpf, Long orgId);
    
    Optional<Paciente> findByPaciEmailAndOrganizacao_Id(String email, Long orgId);
}
