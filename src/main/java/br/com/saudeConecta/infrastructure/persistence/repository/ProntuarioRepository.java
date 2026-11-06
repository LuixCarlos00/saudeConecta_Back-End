package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario, Long> {

    Prontuario findByConsulta_Id(Long consultaId);

    /**
     * Indica se a consulta já tem prontuário finalizado (regra: um prontuário por consulta).
     *
     * @param consultaId ID da consulta
     * @return true se já existe prontuário para a consulta
     */
    boolean existsByConsulta_Id(Long consultaId);
    
    /**
     * Busca um prontuário médico somente se a consulta pertencer à organização informada.
     *
     * @param id    ID do prontuário
     * @param orgId ID da organização do token
     * @return prontuário, ou vazio se não existir ou for de outra clínica
     */
    Optional<Prontuario> findByProntCodigoProntuarioAndConsulta_Organizacao_Id(Long id, Long orgId);

    @Query("SELECT p FROM Prontuario p " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.consulta c " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "LEFT JOIN FETCH p.planejamentos " +
           "WHERE c.id = :consultaId AND c.organizacao.id = :orgId")
    Prontuario findByConsultaIdAndOrganizacaoIdWithFetch(@Param("consultaId") Long consultaId,
                                                         @Param("orgId") Long orgId);

    /**
     * Busca apenas o ID do prontuário mais recente de uma consulta da organização informada.
     * Permite usar LIMIT sem warning de coleção.
     */
    @Query("""
        SELECT p.prontCodigoProntuario FROM Prontuario p
        WHERE p.consulta.id = :consultaId AND p.consulta.organizacao.id = :orgId
        ORDER BY p.prontDataFinalizado DESC
        LIMIT 1
    """)
    Optional<Long> findIdMaisRecentePorConsulta(@Param("consultaId") Long consultaId, @Param("orgId") Long orgId);

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
