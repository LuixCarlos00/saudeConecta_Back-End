package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanejamentoTerapeuticoRepository extends JpaRepository<PlanejamentoTerapeutico, Long> {

    // ==========================================
    // ESTATÍSTICAS FINANCEIRAS (SALDO)
    // ==========================================

    /**
     * Soma o valor dos procedimentos terapêuticos por organização em um período.
     * @return [somaValor, quantidade]
     */
    @Query("SELECT COALESCE(SUM(p.valor), 0), COUNT(p) FROM PlanejamentoTerapeutico p " +
           "WHERE p.organizacao.id = :orgId " +
           "AND p.valor IS NOT NULL " +
           "AND p.dataProcedimento BETWEEN :inicio AND :fim")
    List<Object[]> somarValorProcedimentosPorOrganizacao(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim);

    /**
     * Soma o valor dos procedimentos agrupados por mês/ano.
     * @return [ano, mes, somaValor, quantidade]
     */
    @Query("SELECT YEAR(p.dataProcedimento), MONTH(p.dataProcedimento), COALESCE(SUM(p.valor), 0), COUNT(p) " +
           "FROM PlanejamentoTerapeutico p " +
           "WHERE p.organizacao.id = :orgId " +
           "AND p.valor IS NOT NULL " +
           "AND p.dataProcedimento BETWEEN :inicio AND :fim " +
           "GROUP BY YEAR(p.dataProcedimento), MONTH(p.dataProcedimento) " +
           "ORDER BY YEAR(p.dataProcedimento), MONTH(p.dataProcedimento)")
    List<Object[]> somarValorProcedimentosAgrupadosPorMes(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim);

    /**
     * Soma o valor dos procedimentos agrupados por semana ISO.
     * @return [ano, semana, somaValor, quantidade]
     */
    @Query("SELECT YEAR(p.dataProcedimento), WEEK(p.dataProcedimento), COALESCE(SUM(p.valor), 0), COUNT(p) " +
           "FROM PlanejamentoTerapeutico p " +
           "WHERE p.organizacao.id = :orgId " +
           "AND p.valor IS NOT NULL " +
           "AND p.dataProcedimento BETWEEN :inicio AND :fim " +
           "GROUP BY YEAR(p.dataProcedimento), WEEK(p.dataProcedimento) " +
           "ORDER BY YEAR(p.dataProcedimento), WEEK(p.dataProcedimento)")
    List<Object[]> somarValorProcedimentosAgrupadosPorSemana(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim);

    @Query("SELECT p FROM PlanejamentoTerapeutico p " +
           "WHERE p.prontuarioDentista.codigo = :prontuarioId " +
           "ORDER BY p.dataProcedimento DESC")
    List<PlanejamentoTerapeutico> findByProntuarioId(@Param("prontuarioId") Long prontuarioId);


    @Query("SELECT p FROM PlanejamentoTerapeutico p " +
           "LEFT JOIN FETCH p.paciente " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.organizacao " +
           "WHERE p.tokenAssinatura = :token " +
           "ORDER BY p.dataProcedimento")
    List<PlanejamentoTerapeutico> findByTokenAssinatura(@Param("token") String token);


    @Modifying
    @Query("UPDATE PlanejamentoTerapeutico p " +
           "SET p.statusAssinatura = 'ASSINADO', " +
           "    p.assinaturaBase64 = :assinatura, " +
           "    p.dataAssinatura = CURRENT_TIMESTAMP " +
           "WHERE p.tokenAssinatura = :token")
    int assinarPorToken(@Param("token") String token, @Param("assinatura") String assinatura);


}
