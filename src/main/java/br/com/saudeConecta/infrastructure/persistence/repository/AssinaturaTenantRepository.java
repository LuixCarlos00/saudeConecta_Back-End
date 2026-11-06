package br.com.saudeConecta.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.com.saudeConecta.infrastructure.persistence.projection.PlataformaProjecoes;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Busca a assinatura vigente da organização: a mais recente (maior id), desde que não esteja
     * CANCELADA. Diferente de {@link #findAssinaturaAtivaByOrganizacaoId}, inclui a SUSPENSA, que
     * precisa enxergar a própria assinatura e gerar o link para voltar a pagar. Com histórico, só
     * a mais recente vale: se ela foi cancelada, uma SUSPENSA mais antiga não volta a ser vigente.
     *
     * @param orgId ID da organização
     * @return a assinatura mais recente, se ela for TRIAL, ATIVA, INADIMPLENTE ou SUSPENSA
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "WHERE a.id = (SELECT MAX(v.id) FROM AssinaturaTenant v WHERE v.organizacao.id = :orgId) " +
           "AND a.status <> 'CANCELADA'")
    Optional<AssinaturaTenant> findAssinaturaVigenteByOrganizacaoId(@Param("orgId") Long orgId);


    List<AssinaturaTenant> findByOrganizacaoId(Long organizacaoId);


    /**
     * Busca a assinatura vinculada a um preapproval do Mercado Pago.
     * Usado pelo webhook e pela rotina de reconciliação.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.mpPreapprovalId = :preapprovalId")
    Optional<AssinaturaTenant> findByMpPreapprovalId(@Param("preapprovalId") String preapprovalId);

    /**
     * Busca trials que vencem até a data informada (inclusive os já vencidos) e ainda
     * não possuem link de pagamento válido (ignora cortesias). Usar "até" em vez de
     * "exatamente em" garante nova tentativa quando a geração falha em um dia.
     *
     * Link válido = preapproval {@code pending} com init_point (o checkout ainda serve) ou
     * {@code authorized} (cartão já cadastrado). Um preapproval pausado/cancelado (ex.: o que
     * sobrou ao remover a cortesia) entra para nova tentativa; o
     * {@code AssinaturaMercadoPagoService#gerarLinkPagamento} abandona o antigo no MP antes de
     * criar o novo, então a nova tentativa não deixa preapproval duplicado ativo.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status = 'TRIAL' " +
           "AND a.cortesia = false " +
           "AND (a.mpPreapprovalId IS NULL OR a.mpStatus IS NULL " +
           "     OR (a.mpStatus <> 'authorized' AND (a.mpStatus <> 'pending' OR a.mpInitPoint IS NULL))) " +
           "AND a.dataVencimento <= :dataAlvo")
    List<AssinaturaTenant> findTrialsSemLinkVencendoAte(@Param("dataAlvo") LocalDate dataAlvo);

    /**
     * Busca assinaturas já vinculadas ao Mercado Pago para reconciliação de status e
     * recuperação de faturas. Cortesias ficam de fora: não são cobradas nem renovadas.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.planoAssinatura " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.mpPreapprovalId IS NOT NULL " +
           "AND a.status <> 'CANCELADA' " +
           "AND a.cortesia = false")
    List<AssinaturaTenant> findComPreapprovalParaReconciliar();

    /**
     * Assinatura que ainda guarda o preapproval na coluna de substituído (cancelamento pendente).
     * Usada para ligar à clínica a pendência de fatura aprovada desse preapproval.
     */
    Optional<AssinaturaTenant> findFirstByMpPreapprovalSubstituidoId(String preapprovalId);

    /**
     * IDs das assinaturas com preapproval trocado cujo cancelamento no Mercado Pago ainda
     * não foi confirmado (falhou depois do commit da troca). A reconciliação tenta de novo.
     */
    @Query("SELECT a.id FROM AssinaturaTenant a WHERE a.mpPreapprovalSubstituidoId IS NOT NULL")
    List<Long> findIdsComPreapprovalSubstituidoPendente();

    /**
     * IDs das cortesias que ainda têm preapproval vinculado (dadas antes de a cortesia passar a
     * desvincular o preapproval). Esse preapproval pode seguir cobrando; a reconciliação o limpa.
     */
    @Query("SELECT a.id FROM AssinaturaTenant a WHERE a.cortesia = true AND a.mpPreapprovalId IS NOT NULL")
    List<Long> findIdsCortesiaComPreapproval();

    /**
     * Busca assinaturas vencidas que ainda não foram marcadas como inadimplentes.
     * TRIAL vencido segue a mesma régua da ATIVA vencida. Cortesias são ignoradas.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status IN ('ATIVA', 'TRIAL') " +
           "AND a.cortesia = false " +
           "AND a.dataVencimento < :hoje")
    List<AssinaturaTenant> findVencidasNaoInadimplentes(@Param("hoje") LocalDate hoje);

    /**
     * Busca assinaturas inadimplentes há mais de N dias (para suspender).
     * Cortesias são ignoradas.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "JOIN FETCH a.organizacao " +
           "WHERE a.status = 'INADIMPLENTE' " +
           "AND a.cortesia = false " +
           "AND a.dataVencimento < :dataLimite")
    List<AssinaturaTenant> findInadimplentesParaSuspender(@Param("dataLimite") LocalDate dataLimite);

    /**
     * Busca assinaturas suspensas há mais de N dias (para cancelar), contados a partir
     * da data da suspensão. atualizadoEm só é usado para linhas suspensas por versões
     * anteriores da aplicação, que não gravavam data_suspensao. Cortesias são ignoradas.
     */
    @Query("SELECT a FROM AssinaturaTenant a " +
           "WHERE a.status = 'SUSPENSA' " +
           "AND a.cortesia = false " +
           "AND COALESCE(a.dataSuspensao, a.atualizadoEm) < :dataLimite")
    List<AssinaturaTenant> findSuspensasParaCancelar(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Relê a assinatura com lock de escrita, para a régua decidir sobre o estado atual
     * da linha (e não sobre a foto lida no início do lote).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AssinaturaTenant a WHERE a.id = :id")
    Optional<AssinaturaTenant> findByIdParaAtualizacao(@Param("id") Long id);

    /**
     * Assinatura vigente (a mais recente) de cada clínica, com filtros opcionais, para a tela de
     * assinaturas do SUPER_ADMIN. Visão global, sem filtro de tenant. A ordenação vem do Pageable
     * (o service só aceita campos da lista branca).
     *
     * @param status   status da assinatura (null = todos)
     * @param plano    tipo do plano base (null = todos)
     * @param cortesia true/false (null = todas)
     * @param busca    trecho do nome da clínica já em minúsculas, com % e _ escapados e envolto em %
     *                 (null = sem filtro)
     * @param pageable página, tamanho e ordenação
     * @return página de assinaturas com organização e plano carregados
     */
    @Query(value = "SELECT a FROM AssinaturaTenant a JOIN FETCH a.organizacao o JOIN FETCH a.planoAssinatura p " +
           "WHERE " + VIGENTE_LISTAGEM + FILTROS_LISTAGEM,
           countQuery = "SELECT COUNT(a) FROM AssinaturaTenant a JOIN a.organizacao o JOIN a.planoAssinatura p " +
           "WHERE " + VIGENTE_LISTAGEM + FILTROS_LISTAGEM)
    Page<AssinaturaTenant> listarVigentesParaPlataforma(@Param("status") StatusAssinatura status,
                                                       @Param("plano") TipoPlano plano,
                                                       @Param("cortesia") Boolean cortesia,
                                                       @Param("busca") String busca,
                                                       Pageable pageable);

    String VIGENTE_LISTAGEM = "a.id = (SELECT MAX(v.id) FROM AssinaturaTenant v WHERE v.organizacao.id = o.id)";
    String FILTROS_LISTAGEM = " AND (:status IS NULL OR a.status = :status)" +
            " AND (:plano IS NULL OR p.tipo = :plano)" +
            " AND (:cortesia IS NULL OR a.cortesia = :cortesia)" +
            " AND (:busca IS NULL OR LOWER(o.nome) LIKE :busca ESCAPE '!')";

    // ===== Dashboard da plataforma (SUPER_ADMIN): visão global, sem filtro de tenant =====

    /**
     * Assinatura vigente da clínica = a mais recente. Uma clínica que cancelou e assinou de novo
     * guarda o histórico; contar todas as linhas duplicaria a clínica nos totais por status.
     */
    String VIGENTE = "a.id = (SELECT MAX(v.id) FROM AssinaturaTenant v WHERE v.organizacao.id = a.organizacao.id)";

    /** Clínicas por status da assinatura vigente (status sem clínica não aparece). */
    @Query("SELECT a.status AS status, COUNT(a) AS quantidade FROM AssinaturaTenant a " +
           "WHERE " + VIGENTE + " GROUP BY a.status")
    List<PlataformaProjecoes.ContagemPorStatus> contarVigentesPorStatus();

    /** Clínicas ativas (TRIAL, ATIVA, INADIMPLENTE, cortesia incluída) por plano base. */
    @Query("SELECT p.tipo AS plano, COUNT(a) AS quantidade FROM AssinaturaTenant a " +
           "JOIN a.planoAssinatura p " +
           "WHERE a.status IN ('TRIAL', 'ATIVA', 'INADIMPLENTE') AND " + VIGENTE + " GROUP BY p.tipo")
    List<PlataformaProjecoes.ContagemPorPlano> contarAtivasPorPlano();

    /** MRR por plano base: ATIVA e INADIMPLENTE, sem cortesia. */
    @Query("SELECT p.tipo AS plano, COUNT(a) AS quantidade, SUM(a.valorMensal) AS mrr " +
           "FROM AssinaturaTenant a JOIN a.planoAssinatura p " +
           "WHERE a.status IN ('ATIVA', 'INADIMPLENTE') AND a.cortesia = false AND " + VIGENTE +
           " GROUP BY p.tipo")
    List<PlataformaProjecoes.MrrPorPlano> somarMrrPorPlano();

    /** Clínicas ativas que estão em cortesia. */
    @Query("SELECT COUNT(a) FROM AssinaturaTenant a " +
           "WHERE a.cortesia = true AND a.status IN ('TRIAL', 'ATIVA', 'INADIMPLENTE') AND " + VIGENTE)
    long contarCortesiasAtivas();

    /** Trials com vencimento entre as datas (inclusive). */
    @Query("SELECT COUNT(a) FROM AssinaturaTenant a " +
           "WHERE a.status = 'TRIAL' AND a.dataVencimento BETWEEN :de AND :ate AND " + VIGENTE)
    long contarTrialsVencendo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Assinaturas criadas por mês no intervalo [inicio, fim). */
    @Query("SELECT YEAR(a.criadoEm) AS ano, MONTH(a.criadoEm) AS mes, COUNT(a) AS quantidade " +
           "FROM AssinaturaTenant a WHERE a.criadoEm >= :inicio AND a.criadoEm < :fim " +
           "GROUP BY YEAR(a.criadoEm), MONTH(a.criadoEm)")
    List<PlataformaProjecoes.ContagemMensal> contarCriadasPorMes(@Param("inicio") LocalDateTime inicio,
                                                                 @Param("fim") LocalDateTime fim);

    /** Assinaturas canceladas por mês no intervalo [inicio, fim). */
    @Query("SELECT YEAR(a.dataCancelamento) AS ano, MONTH(a.dataCancelamento) AS mes, COUNT(a) AS quantidade " +
           "FROM AssinaturaTenant a WHERE a.dataCancelamento >= :inicio AND a.dataCancelamento < :fim " +
           "GROUP BY YEAR(a.dataCancelamento), MONTH(a.dataCancelamento)")
    List<PlataformaProjecoes.ContagemMensal> contarCanceladasPorMes(@Param("inicio") LocalDateTime inicio,
                                                                    @Param("fim") LocalDateTime fim);

    /** Clínicas vigentes em um status, pelo vencimento mais antigo (limite pelo Pageable). */
    @Query("SELECT o.id AS organizacaoId, o.nome AS nome, p.tipo AS plano, a.status AS status, " +
           "a.dataVencimento AS dataVencimento, a.valorMensal AS valorMensal " +
           "FROM AssinaturaTenant a JOIN a.organizacao o JOIN a.planoAssinatura p " +
           "WHERE a.status = :status AND " + VIGENTE + " ORDER BY a.dataVencimento ASC, o.id ASC")
    List<PlataformaProjecoes.ClinicaAtencao> buscarAtencaoPorStatus(@Param("status") StatusAssinatura status,
                                                                    Pageable pageable);

    /** Trials com vencimento entre as datas, do mais próximo do vencimento (limite pelo Pageable). */
    @Query("SELECT o.id AS organizacaoId, o.nome AS nome, p.tipo AS plano, a.status AS status, " +
           "a.dataVencimento AS dataVencimento, a.valorMensal AS valorMensal " +
           "FROM AssinaturaTenant a JOIN a.organizacao o JOIN a.planoAssinatura p " +
           "WHERE a.status = 'TRIAL' AND a.dataVencimento BETWEEN :de AND :ate AND " + VIGENTE +
           " ORDER BY a.dataVencimento ASC, o.id ASC")
    List<PlataformaProjecoes.ClinicaAtencao> buscarTrialsVencendo(@Param("de") LocalDate de,
                                                                  @Param("ate") LocalDate ate,
                                                                  Pageable pageable);
}
