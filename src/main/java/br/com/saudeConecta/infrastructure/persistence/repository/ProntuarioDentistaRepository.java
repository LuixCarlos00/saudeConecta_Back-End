package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * PASSO 1 — busca apenas o ID do prontuário mais recente (sem fetch de coleção).
     * Permite usar LIMIT sem o warning HHH90003004.
     */
    @Query("""
        SELECT pd.id FROM ProntuarioDentista pd
        WHERE pd.consulta.id = :consultaId
        ORDER BY pd.dataFinalizado DESC
        LIMIT 1
    """)
    Optional<Long> findIdMaisRecentePorConsulta(@Param("consultaId") Long consultaId);


    @Query("""
    SELECT pd FROM ProntuarioDentista pd
    LEFT JOIN FETCH pd.dentes
    LEFT JOIN FETCH pd.profissional prof
    LEFT JOIN FETCH prof.tipoProfissional
    LEFT JOIN FETCH prof.especialidades
    LEFT JOIN FETCH pd.consulta c
    LEFT JOIN FETCH c.paciente
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
        JOIN FETCH pd.consulta
        WHERE pd.profissional.id = :profissionalId
        ORDER BY pd.dataFinalizado DESC
    """)
    List<ProntuarioDentista> findByProfissionalId(@Param("profissionalId") Long profissionalId);

    /**
     * Verifica se já existe prontuário para uma consulta.
     */
    boolean existsByConsultaId(Long consultaId);
}