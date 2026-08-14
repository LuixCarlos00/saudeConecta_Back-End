package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.suporte.ChamadoSuporte;
import br.com.saudeConecta.domain.suporte.PrioridadeChamado;
import br.com.saudeConecta.domain.suporte.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acesso aos chamados de suporte.
 */
@Repository
public interface ChamadoSuporteRepository extends JpaRepository<ChamadoSuporte, String> {

    /**
     * Lista chamados de uma organizacao com filtros opcionais de status e prioridade.
     *
     * @param orgId      ID da organizacao (tenant)
     * @param status     filtro por status (opcional)
     * @param prioridade filtro por prioridade (opcional)
     * @param pageable   paginacao
     * @return pagina de chamados da organizacao
     */
    @Query("SELECT c FROM ChamadoSuporte c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:prioridade IS NULL OR c.prioridade = :prioridade)")
    Page<ChamadoSuporte> findByOrganizacaoWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") StatusChamado status,
            @Param("prioridade") PrioridadeChamado prioridade,
            Pageable pageable);

    /**
     * Lista chamados de todas as organizacoes (visao do ROOT) com filtros opcionais.
     *
     * @param status     filtro por status (opcional)
     * @param prioridade filtro por prioridade (opcional)
     * @param pageable   paginacao
     * @return pagina de chamados globais
     */
    @Query("SELECT c FROM ChamadoSuporte c " +
           "WHERE (:status IS NULL OR c.status = :status) " +
           "AND (:prioridade IS NULL OR c.prioridade = :prioridade)")
    Page<ChamadoSuporte> findAllWithFilters(
            @Param("status") StatusChamado status,
            @Param("prioridade") PrioridadeChamado prioridade,
            Pageable pageable);

    /**
     * Busca um chamado com os anexos carregados em uma unica query.
     *
     * @param id ID do chamado
     * @return Optional com o chamado e seus anexos
     */
    @Query("SELECT c FROM ChamadoSuporte c LEFT JOIN FETCH c.anexos WHERE c.id = :id")
    Optional<ChamadoSuporte> findByIdWithAnexos(@Param("id") String id);
}
