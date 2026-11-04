package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.planos.MpWebhookEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Grava a falha de processamento só se o evento ainda estiver aberto: uma entrega
     * concorrente que já encerrou o evento não pode ser reaberta por esta escrita.
     *
     * @param id               ID do evento
     * @param erro             mensagem do erro (já truncada)
     * @param proximaTentativa quando o job de reprocessamento pode tentar de novo
     * @return linhas afetadas (0 quando o evento já estava encerrado)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MpWebhookEvento e SET e.erro = :erro, e.tentativas = e.tentativas + 1, " +
           "e.proximaTentativaEm = :proximaTentativa " +
           "WHERE e.id = :id AND e.processado = false")
    int registrarFalha(@Param("id") Long id, @Param("erro") String erro,
                       @Param("proximaTentativa") LocalDateTime proximaTentativa);

    /**
     * Eventos com erro ainda dentro do prazo e cujo backoff já venceu.
     *
     * @param recebidoDesde eventos recebidos antes disso não são mais reprocessados
     * @param agora         instante de referência do backoff
     */
    @Query("SELECT e FROM MpWebhookEvento e " +
           "WHERE e.processado = false AND e.erro IS NOT NULL AND e.expiradoEm IS NULL " +
           "AND e.recebidoEm >= :recebidoDesde " +
           "AND (e.proximaTentativaEm IS NULL OR e.proximaTentativaEm <= :agora) " +
           "ORDER BY e.recebidoEm ASC")
    List<MpWebhookEvento> findComErroParaReprocessar(@Param("recebidoDesde") LocalDateTime recebidoDesde,
                                                     @Param("agora") LocalDateTime agora);

    /**
     * Marca como expirados (e para tratamento manual) os eventos com erro mais antigos que o
     * prazo de reprocessamento.
     *
     * @param recebidoAntesDe limite de idade
     * @param agora           data gravada como expiração
     * @return quantidade de eventos expirados agora
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MpWebhookEvento e SET e.expiradoEm = :agora, e.requerAtencao = true, " +
           "e.motivoAtencao = '" + MpWebhookEvento.MOTIVO_EVENTO_EXPIRADO
           + ": reprocessamento automatico esgotado, conferir o recurso no Mercado Pago' " +
           "WHERE e.processado = false AND e.erro IS NOT NULL AND e.expiradoEm IS NULL " +
           "AND e.recebidoEm < :recebidoAntesDe")
    int expirarComErroAntesDe(@Param("recebidoAntesDe") LocalDateTime recebidoAntesDe,
                              @Param("agora") LocalDateTime agora);

    /**
     * Eventos que precisam de tratamento manual, do mais recente para o mais antigo.
     */
    List<MpWebhookEvento> findByRequerAtencaoTrueOrderByRecebidoEmDesc();
}
