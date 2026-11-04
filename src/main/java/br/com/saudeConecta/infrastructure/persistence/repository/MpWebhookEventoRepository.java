package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MpWebhookEventoRepository extends JpaRepository<MpWebhookEvento, Long> {

    /**
     * Busca um evento já recebido pelo par (tipo, dataId) — base da idempotência.
     */
    Optional<MpWebhookEvento> findByTipoAndDataId(String tipo, String dataId);

    /**
     * Relê o evento com lock de escrita para serializar entregas simultâneas
     * da mesma notificação durante o processamento.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM MpWebhookEvento e WHERE e.id = :id")
    Optional<MpWebhookEvento> findParaProcessamento(@Param("id") Long id);
}
