package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssinaturaTenantRepository extends JpaRepository<AssinaturaTenant, Long> {

    /**
     * Busca a assinatura ativa (TRIAL ou ATIVA) de uma organização.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "WHERE a.organizacao.id = :orgId " +
           "AND a.status IN ('TRIAL', 'ATIVA', 'INADIMPLENTE')")
    Optional<AssinaturaTenant> findAssinaturaAtivaByOrganizacaoId(@Param("orgId") Long orgId);


    List<AssinaturaTenant> findByOrganizacaoId(Long organizacaoId);


    /**
     * Busca assinaturas por lista de status (para geração automática de cobranças).
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status IN :statuses")
    List<AssinaturaTenant> findByStatusIn(@Param("statuses") List<StatusAssinatura> statuses);

    /**
     * Busca assinaturas com vencimento dentro de um intervalo (para gerar cobranças).
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status IN ('ATIVA', 'TRIAL') " +
           "AND a.dataVencimento BETWEEN :inicio AND :fim")
    List<AssinaturaTenant> findVencendoNoIntervalo(
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    /**
     * Busca assinaturas vencidas que ainda não foram marcadas como inadimplentes.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status = 'ATIVA' " +
           "AND a.dataVencimento < :hoje")
    List<AssinaturaTenant> findVencidasNaoInadimplentes(@Param("hoje") LocalDate hoje);

    /**
     * Busca assinaturas inadimplentes há mais de N dias (para suspender).
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status = 'INADIMPLENTE' " +
           "AND a.dataVencimento < :dataLimite")
    List<AssinaturaTenant> findInadimplentesParaSuspender(@Param("dataLimite") LocalDate dataLimite);

    /**
     * Busca assinaturas suspensas há mais de N dias (para cancelar).
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "WHERE a.status = 'SUSPENSA' " +
           "AND a.atualizadoEm < :dataLimite")
    List<AssinaturaTenant> findSuspensasParaCancelar(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca todas as assinaturas com dados do plano e organização (para listagem SUPER_ADMIN).
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH a.organizacao " +
           "ORDER BY a.criadoEm DESC")
    List<AssinaturaTenant> findAllWithRelations();
}
