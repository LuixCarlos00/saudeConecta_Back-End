package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario, Long> {

    Prontuario findByConsulta_Id(Long consultaId);
    
    List<Prontuario> findByConsulta_Paciente_PaciCodigo(Long paciCodigo);
    
    List<Prontuario> findByProfissional_Id(Long profissionalId);
    
    @Query("SELECT p FROM Prontuario p " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.consulta c " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "LEFT JOIN FETCH p.planejamentos " +
           "WHERE p.consulta.id = :consultaId")
    Prontuario findByConsulta_IdWithFetch(Long consultaId);

    /**
     * Busca apenas o ID do prontuário mais recente de uma consulta.
     * Permite usar LIMIT sem warning de coleção.
     */
    @Query("""
        SELECT p.prontCodigoProntuario FROM Prontuario p
        WHERE p.consulta.id = :consultaId
        ORDER BY p.prontDataFinalizado DESC
        LIMIT 1
    """)
    Optional<Long> findIdMaisRecentePorConsulta(@Param("consultaId") Long consultaId);

    /**
     * Busca prontuário por ID com fetch completo de relacionamentos.
     */
    @Query("SELECT p FROM Prontuario p " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.consulta c " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "LEFT JOIN FETCH p.planejamentos " +
           "WHERE p.prontCodigoProntuario = :id")
    Optional<Prontuario> findByIdWithFullFetch(@Param("id") Long id);

    /**
     * Exclui todos os prontuários médicos vinculados a uma consulta.
     */
    @Modifying
    @Query("DELETE FROM Prontuario p WHERE p.consulta.id = :consultaId")
    void deleteByConsultaId(@Param("consultaId") Long consultaId);
}
