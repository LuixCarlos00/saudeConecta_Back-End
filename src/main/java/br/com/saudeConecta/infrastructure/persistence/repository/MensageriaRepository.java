package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório para acesso aos dados de mensageria.
 */
@Repository
public interface MensageriaRepository extends JpaRepository<Mensageria, Long> {

    Page<Mensageria> findByOrganizacao_IdOrderByDataCriacaoDesc(Long organizacaoId, Pageable pageable);

    List<Mensageria> findByOrganizacao_IdAndStatusOrderByDataCriacaoDesc(Long organizacaoId, StatusMensagem status);

    List<Mensageria> findByOrganizacao_IdAndAdminNotificadoFalseAndStatus(Long organizacaoId, StatusMensagem status);

    @Query(value = "SELECT m FROM Mensageria m LEFT JOIN FETCH m.destinatarioProfissional " +
                   "WHERE m.organizacao.id = :orgId ORDER BY m.dataCriacao DESC",
           countQuery = "SELECT COUNT(m) FROM Mensageria m WHERE m.organizacao.id = :orgId")
    Page<Mensageria> findByOrganizacaoIdWithProfissional(@Param("orgId") Long orgId, Pageable pageable);

    @Query("SELECT m FROM Mensageria m WHERE m.organizacao.id = :orgId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:tipo IS NULL OR m.tipoMensagem = :tipo) " +
           "ORDER BY m.dataCriacao DESC")
    Page<Mensageria> findByOrganizacaoIdWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") StatusMensagem status,
            @Param("tipo") TipoMensagem tipo,
            Pageable pageable);

    long countByOrganizacao_IdAndStatus(Long organizacaoId, StatusMensagem status);

    long countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(Long organizacaoId, StatusMensagem status);
}
