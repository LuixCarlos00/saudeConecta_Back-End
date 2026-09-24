package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MpWebhookEventoRepository extends JpaRepository<MpWebhookEvento, Long> {

    /**
     * Busca um evento já recebido pelo par (tipo, dataId) — base da idempotência.
     */
    Optional<MpWebhookEvento> findByTipoAndDataId(String tipo, String dataId);
}
