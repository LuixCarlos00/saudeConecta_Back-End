package br.com.saudeConecta.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import br.com.saudeConecta.infrastructure.persistence.projection.PlataformaProjecoes;
import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaTenantRepository extends JpaRepository<CobrancaTenant, Long> {

    /**
     * Busca a cobrança vinculada a um pagamento autorizado do Mercado Pago.
     * Usado pelo webhook para garantir idempotência no lançamento financeiro.
     */
    Optional<CobrancaTenant> findByMpAuthorizedPaymentId(String mpAuthorizedPaymentId);

    /**
     * Busca cobranças por status ordenadas por data de criação (SuperAdmin).
     */
    @Query("SELECT c FROM CobrancaTenant c " +
           "JOIN FETCH c.assinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH c.organizacao " +
           "WHERE c.status = :status " +
           "ORDER BY c.criadaEm DESC")
    List<CobrancaTenant> findByStatusOrderByCriadaEmDesc(@Param("status") StatusCobranca status);

    /**
     * Busca a cobrança pendente mais recente de uma organização (para dashboard).
     */
    @Query("SELECT c FROM CobrancaTenant c " +
           "JOIN FETCH c.organizacao " +
           "JOIN FETCH c.assinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "AND c.status = :status " +
           "ORDER BY c.criadaEm DESC " +
           "LIMIT 1")
    Optional<CobrancaTenant> findTopByOrganizacaoIdAndStatusOrderByCriadaEmDesc(
            @Param("organizacaoId") Long organizacaoId, 
            @Param("status") StatusCobranca status);

    /**
     * Busca todas as cobranças de um tenant com dados da assinatura (para histórico).
     */
    @Query("SELECT c FROM CobrancaTenant c " +
           "JOIN FETCH c.assinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "WHERE c.organizacao.id = :orgId " +
           "ORDER BY c.criadaEm DESC")
    List<CobrancaTenant> findByOrganizacaoIdWithRelations(@Param("orgId") Long orgId);

    /** Soma das cobranças PAGO com pagamento em [inicio, fim); nulo quando não há nenhuma. */
    @Query("SELECT SUM(c.valorTotal) FROM CobrancaTenant c " +
           "WHERE c.status = 'PAGO' AND c.dataPagamento >= :inicio AND c.dataPagamento < :fim")
    BigDecimal somarRecebidoNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** Receita recebida (PAGO) por mês de pagamento no intervalo [inicio, fim). */
    @Query("SELECT YEAR(c.dataPagamento) AS ano, MONTH(c.dataPagamento) AS mes, SUM(c.valorTotal) AS valor " +
           "FROM CobrancaTenant c " +
           "WHERE c.status = 'PAGO' AND c.dataPagamento >= :inicio AND c.dataPagamento < :fim " +
           "GROUP BY YEAR(c.dataPagamento), MONTH(c.dataPagamento)")
    List<PlataformaProjecoes.ValorMensal> somarRecebidoPorMes(@Param("inicio") LocalDateTime inicio,
                                                              @Param("fim") LocalDateTime fim);
}
