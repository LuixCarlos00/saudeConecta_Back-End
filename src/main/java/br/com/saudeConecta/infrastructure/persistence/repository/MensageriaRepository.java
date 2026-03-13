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


    List<Mensageria> findByOrganizacao_IdAndAdminNotificadoFalseAndStatus(Long organizacaoId, StatusMensagem status);


    @Query("SELECT m FROM Mensageria m WHERE m.organizacao.id = :orgId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:tipo IS NULL OR m.tipoMensagem = :tipo) " +
           "ORDER BY m.dataCriacao DESC")
    Page<Mensageria> findByOrganizacaoIdWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") StatusMensagem status,
            @Param("tipo") TipoMensagem tipo,
            Pageable pageable);


    long countByOrganizacao_IdAndAdminNotificadoFalseAndStatus(Long organizacaoId, StatusMensagem status);


    @Query("SELECT m FROM Mensageria m " +
           "WHERE (:status IS NULL OR m.status = :status) " +
           "AND (:tipo IS NULL OR m.tipoMensagem = :tipo) " +
           "ORDER BY m.dataCriacao DESC")
    Page<Mensageria> findAllWithFilters(
            @Param("status") StatusMensagem status,
            @Param("tipo") TipoMensagem tipo,
            Pageable pageable);


    List<Mensageria> findByAdminNotificadoFalseAndStatus(StatusMensagem status);


    long countByAdminNotificadoFalseAndStatus(StatusMensagem status);
}
