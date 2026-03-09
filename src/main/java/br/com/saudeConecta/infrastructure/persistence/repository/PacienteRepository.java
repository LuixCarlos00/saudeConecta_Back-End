package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    

    // Métodos com tenant (multi-tenant)
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId")
    List<Paciente> findByOrganizacao_Id(@Param("orgId") Long organizacaoId);
    


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



    boolean existsByPaciCpfAndOrganizacao_Id(String cpf, Long orgId);
    



    // Métodos com filtro de status
    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND (:filtro = 'ALL' OR p.paciStatus = :status)")
    List<Paciente> findByOrganizacao_IdWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("filtro") String filtro,
        @Param("status") String status);



    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciNome) LIKE LOWER(CONCAT('%', :nome, '%')) " +
           "AND (:filtro = 'ALL' OR p.paciStatus = :status)")
    List<Paciente> findByOrganizacaoIdAndNomeContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("nome") String nome,
        @Param("filtro") String filtro,
        @Param("status") String status);



    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciCpf) LIKE LOWER(CONCAT('%', :cpf, '%')) " +
           "AND (:filtro = 'ALL' OR p.paciStatus = :status)")
    List<Paciente> findByOrganizacaoIdAndCpfContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("cpf") String cpf,
        @Param("filtro") String filtro,
        @Param("status") String status);



    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciRg) LIKE LOWER(CONCAT('%', :rg, '%')) " +
           "AND (:filtro = 'ALL' OR p.paciStatus = :status)")
    List<Paciente> findByOrganizacaoIdAndRgContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("rg") String rg,
        @Param("filtro") String filtro,
        @Param("status") String status);



    @Query("SELECT p FROM Paciente p LEFT JOIN FETCH p.endereco WHERE p.organizacao.id = :orgId " +
           "AND LOWER(p.paciTelefone) LIKE LOWER(CONCAT('%', :telefone, '%')) " +
           "AND (:filtro = 'ALL' OR p.paciStatus = :status)")
    List<Paciente> findByOrganizacaoIdAndTelefoneContainingWithFiltro(
        @Param("orgId") Long orgId, 
        @Param("telefone") String telefone,
        @Param("filtro") String filtro,
        @Param("status") String status);
}
