package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.planos.CobrancaTenant;
import br.com.saudeConecta.domain.planos.StatusCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaTenantRepository extends JpaRepository<CobrancaTenant, Long> {

    Optional<CobrancaTenant> findByTxid(String txid);

    Optional<CobrancaTenant> findByAsaasPaymentId(String asaasPaymentId);

    List<CobrancaTenant> findByOrganizacaoIdOrderByCriadaEmDesc(Long organizacaoId);

    List<CobrancaTenant> findByAssinaturaTenantIdOrderByCriadaEmDesc(Long assinaturaId);

    /**
     * Busca cobranças com Pix expirado para regenerar.
     */
    @Query("SELECT c FROM CobrancaTenant c " +
           "JOIN FETCH c.assinaturaTenant " +
           "JOIN FETCH c.organizacao " +
           "WHERE c.status = 'PENDENTE' " +
           "AND c.dataVencimentoPix < :hoje")
    List<CobrancaTenant> findPixExpirados(@Param("hoje") LocalDate hoje);

    /**
     * Busca cobranças por status.
     */
    List<CobrancaTenant> findByStatus(StatusCobranca status);

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
     * Verifica se já existe cobrança pendente para a assinatura.
     */
    boolean existsByAssinaturaTenantIdAndStatus(Long assinaturaId, StatusCobranca status);

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
}
