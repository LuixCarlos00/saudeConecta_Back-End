package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermoAutorizacaoRepository extends JpaRepository<TermoAutorizacao, Long> {

    Optional<TermoAutorizacao> findByToken(String token);

    Optional<TermoAutorizacao> findByConsultaId(Long consultaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TermoAutorizacao t WHERE t.consulta.id = :consultaId")
    Optional<TermoAutorizacao> findByConsultaIdWithLock(@Param("consultaId") Long consultaId);

    @Query("SELECT t FROM TermoAutorizacao t " +
           "LEFT JOIN FETCH t.consulta c " +
           "LEFT JOIN FETCH c.organizacao o " +
           "LEFT JOIN FETCH c.profissional pr " +
           "LEFT JOIN FETCH t.paciente p " +
           "WHERE t.token = :token")
    Optional<TermoAutorizacao> findByTokenComRelacionamentos(@Param("token") String token);

    /**
     * Exclui todos os termos de autorização/questionários vinculados a uma consulta.
     */
    @Modifying
    @Query("DELETE FROM TermoAutorizacao t WHERE t.consulta.id = :consultaId")
    void deleteByConsultaId(@Param("consultaId") Long consultaId);
}
