package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.ProcedimentoPadrao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedimentoPadraoRepository extends JpaRepository<ProcedimentoPadrao, Long> {

    @Query("SELECT p FROM ProcedimentoPadrao p " +
           "WHERE p.profissional.id = :profissionalId " +
           "AND p.organizacao.id = :orgId " +
           "AND p.ativo = true " +
           "ORDER BY p.nomeProcedimento")
    List<ProcedimentoPadrao> findAtivosByProfissionalAndOrg(
            @Param("profissionalId") Long profissionalId,
            @Param("orgId") Long orgId);

    @Query("SELECT p FROM ProcedimentoPadrao p " +
           "WHERE p.profissional.id = :profissionalId " +
           "AND p.organizacao.id = :orgId " +
           "ORDER BY p.ativo DESC, p.nomeProcedimento")
    List<ProcedimentoPadrao> findAllByProfissionalAndOrg(
            @Param("profissionalId") Long profissionalId,
            @Param("orgId") Long orgId);

}
