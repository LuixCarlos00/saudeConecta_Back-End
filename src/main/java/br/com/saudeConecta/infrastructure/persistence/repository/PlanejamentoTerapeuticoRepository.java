package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanejamentoTerapeuticoRepository extends JpaRepository<PlanejamentoTerapeutico, Long> {

    @Query("SELECT p FROM PlanejamentoTerapeutico p " +
           "WHERE p.prontuarioDentista.codigo = :prontuarioId " +
           "ORDER BY p.dataProcedimento DESC")
    List<PlanejamentoTerapeutico> findByProntuarioId(@Param("prontuarioId") Long prontuarioId);


    @Query("SELECT p FROM PlanejamentoTerapeutico p " +
           "LEFT JOIN FETCH p.paciente " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.organizacao " +
           "WHERE p.tokenAssinatura = :token " +
           "ORDER BY p.dataProcedimento")
    List<PlanejamentoTerapeutico> findByTokenAssinatura(@Param("token") String token);


    @Modifying
    @Query("UPDATE PlanejamentoTerapeutico p " +
           "SET p.statusAssinatura = 'ASSINADO', " +
           "    p.assinaturaBase64 = :assinatura, " +
           "    p.dataAssinatura = CURRENT_TIMESTAMP " +
           "WHERE p.tokenAssinatura = :token")
    int assinarPorToken(@Param("token") String token, @Param("assinatura") String assinatura);


}
