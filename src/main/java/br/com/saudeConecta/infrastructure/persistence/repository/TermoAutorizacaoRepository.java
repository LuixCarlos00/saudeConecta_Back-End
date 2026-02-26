package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermoAutorizacaoRepository extends JpaRepository<TermoAutorizacao, Long> {

    Optional<TermoAutorizacao> findByToken(String token);

    Optional<TermoAutorizacao> findByConsultaId(Long consultaId);

    @Query("SELECT t FROM TermoAutorizacao t " +
           "LEFT JOIN FETCH t.consulta c " +
           "LEFT JOIN FETCH t.paciente p " +
           "WHERE t.token = :token")
    Optional<TermoAutorizacao> findByTokenComRelacionamentos(@Param("token") String token);

    boolean existsByConsultaId(Long consultaId);
}
