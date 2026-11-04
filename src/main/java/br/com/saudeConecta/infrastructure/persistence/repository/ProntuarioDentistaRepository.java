package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface ProntuarioDentistaRepository extends JpaRepository<ProntuarioDentista, Long> {

    /**
     * Lista prontuários de uma consulta, já carregando os dentes do odontograma.
     */
    @Query("""
        SELECT DISTINCT pd FROM ProntuarioDentista pd
        LEFT JOIN FETCH pd.dentes
        JOIN FETCH pd.profissional
        WHERE pd.consulta.id = :consultaId
        ORDER BY pd.dataFinalizado DESC
    """)
    List<ProntuarioDentista> findByConsultaId(@Param("consultaId") Long consultaId);

    /**
     * Lista prontuários de uma consulta da organização informada, com os dentes do odontograma.
     *
     * @param consultaId ID da consulta
     * @param orgId      ID da organização do token
     * @return prontuários da consulta, ou vazio se a consulta for de outra clínica
     */
    @Query("""
        SELECT DISTINCT pd FROM ProntuarioDentista pd
        LEFT JOIN FETCH pd.dentes
        JOIN FETCH pd.profissional
        WHERE pd.consulta.id = :consultaId AND pd.consulta.organizacao.id = :orgId
        ORDER BY pd.dataFinalizado DESC
    """)
    List<ProntuarioDentista> findByConsultaIdAndOrganizacaoId(@Param("consultaId") Long consultaId,
                                                              @Param("orgId") Long orgId);

    /**
     * Busca um prontuário odontológico somente se a consulta pertencer à organização informada.
     *
     * @param codigo ID do prontuário
     * @param orgId  ID da organização do token
     * @return prontuário, ou vazio se não existir ou for de outra clínica
     */
    Optional<ProntuarioDentista> findByCodigoAndConsulta_Organizacao_Id(Long codigo, Long orgId);

    /**
     * PASSO 1 — busca apenas o ID do prontuário mais recente (sem fetch de coleção).
     * Permite usar LIMIT sem o warning HHH90003004.
     */
    @Query("""
        SELECT pd.id FROM ProntuarioDentista pd
        WHERE pd.consulta.id = :consultaId AND pd.consulta.organizacao.id = :orgId
        ORDER BY pd.dataFinalizado DESC
        LIMIT 1
    """)
    Optional<Long> findIdMaisRecentePorConsulta(@Param("consultaId") Long consultaId, @Param("orgId") Long orgId);


    @Query("""
    SELECT DISTINCT pd FROM ProntuarioDentista pd
    LEFT JOIN FETCH pd.dentes
    LEFT JOIN FETCH pd.profissional prof
    LEFT JOIN FETCH prof.tipoProfissional
    LEFT JOIN FETCH prof.especialidades
    LEFT JOIN FETCH pd.consulta c
    LEFT JOIN FETCH c.paciente
    LEFT JOIN FETCH c.especialidade
    LEFT JOIN FETCH c.formaPagamento
    WHERE pd.id = :id
""")
    Optional<ProntuarioDentista> findByIdComDentes(@Param("id") Long id);




    /**
     * Lista prontuários de um profissional.
     */
    @Query("""
        SELECT DISTINCT pd FROM ProntuarioDentista pd
        LEFT JOIN FETCH pd.dentes
        JOIN FETCH pd.consulta c
        WHERE pd.profissional.id = :profissionalId AND c.organizacao.id = :orgId
        ORDER BY pd.dataFinalizado DESC
    """)
    List<ProntuarioDentista> findByProfissionalId(@Param("profissionalId") Long profissionalId,
                                                  @Param("orgId") Long orgId);


    /**
     * Lista prontuários de um paciente (via consulta → paciente) na organização informada.
     *
     * @param pacienteId ID do paciente
     * @param orgId      ID da organização do token
     * @return lista de prontuários do paciente ordenados por data
     */
    @Query("""
        SELECT DISTINCT pd FROM ProntuarioDentista pd
        LEFT JOIN FETCH pd.dentes
        JOIN FETCH pd.consulta c
        LEFT JOIN FETCH c.paciente
        LEFT JOIN FETCH c.especialidade
        LEFT JOIN FETCH c.formaPagamento
        JOIN FETCH pd.profissional prof
        LEFT JOIN FETCH prof.tipoProfissional
        LEFT JOIN FETCH prof.especialidades
        WHERE c.paciente.id = :pacienteId AND c.organizacao.id = :orgId
        ORDER BY pd.dataFinalizado DESC
    """)
    List<ProntuarioDentista> findByPacienteId(@Param("pacienteId") Long pacienteId, @Param("orgId") Long orgId);

    /**
     * Exclui todos os dentes (odontograma) dos prontuários vinculados a uma consulta.
     */
    @Modifying
    @Query("DELETE FROM ProntuarioDentistaDente d WHERE d.prontuarioDentista.codigo IN (SELECT pd.codigo FROM ProntuarioDentista pd WHERE pd.consulta.id = :consultaId)")
    void deleteDentesByConsultaId(@Param("consultaId") Long consultaId);

    /**
     * Exclui todos os planejamentos terapêuticos vinculados aos prontuários de uma consulta.
     */
    @Modifying
    @Query("DELETE FROM PlanejamentoTerapeutico p WHERE p.prontuarioDentista.codigo IN (SELECT pd.codigo FROM ProntuarioDentista pd WHERE pd.consulta.id = :consultaId)")
    void deletePlanejamentosByConsultaId(@Param("consultaId") Long consultaId);

    /**
     * Exclui todos os prontuários dentista vinculados a uma consulta.
     */
    @Modifying
    @Query("DELETE FROM ProntuarioDentista pd WHERE pd.consulta.id = :consultaId")
    void deleteByConsultaId(@Param("consultaId") Long consultaId);
}