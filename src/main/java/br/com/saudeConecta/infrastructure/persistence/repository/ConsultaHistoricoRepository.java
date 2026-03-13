package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.consulta.ConsultaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaHistoricoRepository extends JpaRepository<ConsultaHistorico, Long> {

    /**
     * Exclui todo o histórico de alterações de status de uma consulta.
     */
    @Modifying
    @Query("DELETE FROM ConsultaHistorico ch WHERE ch.consulta.id = :consultaId")
    void deleteByConsultaId(@Param("consultaId") Long consultaId);
}
