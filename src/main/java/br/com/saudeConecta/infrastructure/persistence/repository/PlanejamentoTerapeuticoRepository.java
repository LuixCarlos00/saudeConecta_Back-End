package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
           "WHERE p.prontuarioDentista.codigo = :prontuarioId AND p.organizacao.id = :orgId " +
           "ORDER BY p.dataProcedimento DESC")
    List<PlanejamentoTerapeutico> findByProntuarioId(@Param("prontuarioId") Long prontuarioId,
                                                     @Param("orgId") Long orgId);

    @Query("SELECT p FROM PlanejamentoTerapeutico p " +
           "WHERE p.prontuario.prontCodigoProntuario = :prontuarioId AND p.organizacao.id = :orgId " +
           "ORDER BY p.dataProcedimento DESC")
    List<PlanejamentoTerapeutico> findByProntuarioMedicoId(@Param("prontuarioId") Long prontuarioId,
                                                           @Param("orgId") Long orgId);

    Optional<PlanejamentoTerapeutico> findByIdAndOrganizacao_Id(Long id, Long orgId);


    @Query("SELECT p FROM PlanejamentoTerapeutico p " +
           "LEFT JOIN FETCH p.paciente " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.organizacao " +
           "WHERE p.tokenAssinatura = :token " +
           "ORDER BY p.dataProcedimento")
    List<PlanejamentoTerapeutico> findByTokenAssinatura(@Param("token") String token);


    /**
     * Assina os itens do link apenas se ainda estiverem PENDENTES e o link não tiver expirado.
     * O token é mantido: o link é de assinatura única (reenvio afeta 0 linhas pois o status deixa de ser PENDENTE),
     * mas reabrir o link continua mostrando o planejamento como já assinado.
     * Expiração nula = link legado gerado antes da coluna existir, tratado como sem expiração.
     *
     * @param token      token público de assinatura
     * @param assinatura assinatura digital em base64
     * @param ipOrigem   IP de origem resolvido pelo servidor
     * @param agora      instante da assinatura (também usado para checar a expiração)
     * @return quantidade de itens assinados
     */
    @Modifying
    @Query("UPDATE PlanejamentoTerapeutico p " +
           "SET p.statusAssinatura = 'ASSINADO', " +
           "    p.assinaturaBase64 = :assinatura, " +
           "    p.dataAssinatura = :agora, " +
           "    p.ipOrigem = :ipOrigem " +
           "WHERE p.tokenAssinatura = :token " +
           "AND p.statusAssinatura = 'PENDENTE' " +
           "AND (p.tokenExpiracao IS NULL OR p.tokenExpiracao > :agora)")
    int assinarPorToken(@Param("token") String token, @Param("assinatura") String assinatura,
                        @Param("ipOrigem") String ipOrigem, @Param("agora") LocalDateTime agora);

    /**
     * Exclui planejamentos terapêuticos vinculados diretamente a uma consulta.
     */
    @Modifying
    @Query("DELETE FROM PlanejamentoTerapeutico p WHERE p.consulta.id = :consultaId")
    void deleteByConsultaId(@Param("consultaId") Long consultaId);
}
