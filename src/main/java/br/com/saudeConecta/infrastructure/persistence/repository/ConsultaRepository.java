package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.infrastructure.persistence.projection.PlataformaProjecoes;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long>, JpaSpecificationExecutor<Consulta> {

    Optional<Consulta> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);

    /**
     * Mesma busca de {@link #findByIdAndOrganizacao_Id}, travando a linha (SELECT ... FOR UPDATE) até o
     * fim da transação. Serializa a gravação do rascunho com a finalização do prontuário, que atualiza a
     * mesma linha. Só vale dentro de @Transactional.
     *
     * @param id            ID da consulta
     * @param organizacaoId clínica do token
     * @return consulta travada, ou vazio se não existir na clínica
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Consulta c WHERE c.id = :id AND c.organizacao.id = :organizacaoId")
    Optional<Consulta> buscarComLock(@Param("id") Long id, @Param("organizacaoId") Long organizacaoId);

    // Métodos com JOIN FETCH para evitar LazyInitializationException
    
    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.id = :id AND c.organizacao.id = :organizacaoId")
    Optional<Consulta> findByIdAndOrganizacao_IdWithRelations(
        @Param("id") Long id,
        @Param("organizacaoId") Long organizacaoId);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.dataHora BETWEEN :inicio AND :fim ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndPeriodo(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND (:profissionalId IS NULL OR p.id = :profissionalId) " +
           "AND c.dataHora BETWEEN :inicio AND :fim ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndPeriodo(
        @Param("orgId") Long organizacaoId,
        @Param("profissionalId") Long profissionalId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    /**
     * Consultas que ocupam a agenda (AGENDADA ou CONFIRMADA) de um profissional com início em [de, ate).
     * A sobreposição de intervalo é calculada em Java (soma de minutos em JPQL não é portável entre H2 e MySQL).
     */
    @Query("SELECT c FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND c.status IN ('AGENDADA', 'CONFIRMADA') " +
           "AND c.dataHora >= :de AND c.dataHora < :ate " +
           "ORDER BY c.dataHora")
    List<Consulta> findAtivasDoProfissionalNoPeriodo(@Param("orgId") Long orgId,
                                                      @Param("profissionalId") Long profissionalId,
                                                      @Param("de") LocalDateTime de,
                                                      @Param("ate") LocalDateTime ate);

    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndDataHoraBetweenWithRelations(
        @Param("organizacaoId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
        @Param("orgId") Long orgId,
        @Param("usuarioId") Long usuarioId,
        @Param("status") StatusConsulta status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByStatusOptionalAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("status") StatusConsulta status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    // ========== MÉTODO PARA BUSCAR HORÁRIOS OCUPADOS ==========
    
    @Query("SELECT TIME(c.dataHora) FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND DATE(c.dataHora) = DATE(:data) " +
           "AND (c.status ='AGENDADA' OR c.status ='CONFIRMADA')" + 
           "ORDER BY c.dataHora")
    List<String> findHorariosOcupados(@Param("orgId") Long orgId,
                                      @Param("profissionalId") Long profissionalId,
                                      @Param("data") LocalDateTime data);

    // ==========================================
    // ESTATÍSTICAS POR PROFISSIONAL (usuarioId + orgId) - HOJE
    // ==========================================

    // ==========================================
    // ESTATÍSTICAS POR MÉDICO E INTERVALO
    // ==========================================

    @Query("SELECT c.status, COUNT(c) FROM Consulta c " +
           "JOIN c.profissional p " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "GROUP BY c.status " +
           "ORDER BY c.status")
    List<Object[]> findEstatisticasPorMedicoEIntervalo(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    /**
     * Retorna contagens agrupadas por status e por se é hoje ou semana,
     * para o dashboard do AdminOrg. Uma única query cobre todos os campos necessários.
     * Resultado: [status (String), inicioDia (LocalDateTime), quantidade (Long)]
     */
    @Query("SELECT c.status, " +
           "CASE WHEN c.dataHora >= :inicioDia AND c.dataHora < :fimDia THEN 'HOJE' ELSE 'SEMANA' END, " +
           "COUNT(c) " +
           "FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.dataHora BETWEEN :inicioSemana AND :fimSemana " +
           "GROUP BY c.status, " +
           "CASE WHEN c.dataHora >= :inicioDia AND c.dataHora < :fimDia THEN 'HOJE' ELSE 'SEMANA' END")
    List<Object[]> findEstatisticasDashboardByOrganizacao(
        @Param("orgId") Long organizacaoId,
        @Param("inicioSemana") LocalDateTime inicioSemana,
        @Param("fimSemana") LocalDateTime fimSemana,
        @Param("inicioDia") LocalDateTime inicioDia,
        @Param("fimDia") LocalDateTime fimDia);

    /**
     * Retorna contagens agrupadas por status e período (hoje vs semana) para o SuperAdmin.
     * Sem filtro de organização — abrange todas as consultas do sistema.
     * Resultado: [status (StatusConsulta), periodo (String "HOJE"|"SEMANA"), quantidade (Long)]
     */
    @Query("SELECT c.status, " +
           "CASE WHEN c.dataHora >= :inicioDia AND c.dataHora < :fimDia THEN 'HOJE' ELSE 'SEMANA' END, " +
           "COUNT(c) " +
           "FROM Consulta c " +
           "WHERE c.dataHora BETWEEN :inicioSemana AND :fimSemana " +
           "GROUP BY c.status, " +
           "CASE WHEN c.dataHora >= :inicioDia AND c.dataHora < :fimDia THEN 'HOJE' ELSE 'SEMANA' END")
    List<Object[]> findEstatisticasDashboardGlobal(
        @Param("inicioSemana") LocalDateTime inicioSemana,
        @Param("fimSemana") LocalDateTime fimSemana,
        @Param("inicioDia") LocalDateTime inicioDia,
        @Param("fimDia") LocalDateTime fimDia);

    /**
     * Retorna contagens agrupadas por status e período (hoje vs semana) para o Profissional.
     * Filtra por usuario.id via JOIN com profissional — garante que o profissional veja apenas seus dados.
     * Resultado: [status (StatusConsulta), periodo (String "HOJE"|"SEMANA"), quantidade (Long)]
     */
    @Query("SELECT c.status, " +
           "CASE WHEN c.dataHora >= :inicioDia AND c.dataHora < :fimDia THEN 'HOJE' ELSE 'SEMANA' END, " +
           "COUNT(c) " +
           "FROM Consulta c " +
           "JOIN c.profissional p " +
           "WHERE p.usuario.id = :usuarioId " +
           "AND c.dataHora BETWEEN :inicioSemana AND :fimSemana " +
           "GROUP BY c.status, " +
           "CASE WHEN c.dataHora >= :inicioDia AND c.dataHora < :fimDia THEN 'HOJE' ELSE 'SEMANA' END")
    List<Object[]> findEstatisticasDashboardByProfissional(
        @Param("usuarioId") Long usuarioId,
        @Param("inicioSemana") LocalDateTime inicioSemana,
        @Param("fimSemana") LocalDateTime fimSemana,
        @Param("inicioDia") LocalDateTime inicioDia,
        @Param("fimDia") LocalDateTime fimDia);

    @Query("SELECT c.duracaoMinutos FROM Consulta c " +
           "JOIN c.profissional p " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "AND c.duracaoMinutos IS NOT NULL " +
           "AND c.status = 'REALIZADA'")
    List<Integer> findDuracoesConsultasPorMedicoEIntervalo(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    // ==========================================
    // BUSCAR CONSULTAS POR MÉDICO E PERÍODO
    // ==========================================

    /**
     * Busca consultas de um médico (via usuario.id) em um período específico
     * Retorna todas as consultas com relacionamentos carregados
     * 
     * @param orgId ID da organização
     * @param usuarioId ID do usuário do profissional
     * @param inicio Data/hora inicial do período
     * @param fim Data/hora final do período
     * @return Lista de consultas ordenadas por data/hora
     */
    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.usuario " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findConsultasPorMedicoEPeriodo(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    /**
     * Busca histórico completo de consultas REALIZADAS ou PAGAS de um paciente que POSSUEM prontuário odontológico.
     * Usa INNER JOIN com ProntuarioDentista para garantir que apenas consultas com prontuário dental sejam retornadas.
     * Filtra por organização e, opcionalmente, por profissional.
     * 
     * @param pacienteId ID do paciente
     * @param organizacaoId ID da organização
     * @param profissionalId ID do profissional (opcional, null retorna todos)
     * @return Lista de consultas REALIZADAS ou PAGAS com prontuário dentista, ordenadas por data/hora (mais recente primeiro)
     */
    @Query("SELECT DISTINCT c FROM Consulta c " +
           "INNER JOIN ProntuarioDentista pr ON pr.consulta.id = c.id " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "LEFT JOIN FETCH c.paciente pac " +
           "LEFT JOIN FETCH c.especialidade " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "AND c.paciente.paciCodigo = :pacienteId " +
           "AND (:profissionalId IS NULL OR p.id = :profissionalId) " +
           "AND c.status IN ('REALIZADA', 'PAGO') " +
           "ORDER BY c.dataHora DESC")
    List<Consulta> findHistoricoCompletoPaciente(
        @Param("pacienteId") Long pacienteId,
        @Param("organizacaoId") Long organizacaoId,
        @Param("profissionalId") Long profissionalId);

    /**
     * Busca histórico completo de consultas REALIZADAS ou PAGAS de um paciente que POSSUEM prontuário médico.
     * Usa INNER JOIN com Prontuario para garantir que apenas consultas com prontuário médico sejam retornadas.
     * Filtra por organização e, opcionalmente, por profissional.
     *
     * @param pacienteId ID do paciente
     * @param organizacaoId ID da organização
     * @param profissionalId ID do profissional (opcional, null retorna todos)
     * @return Lista de consultas com prontuário médico, ordenadas por data/hora (mais recente primeiro)
     */
    @Query("SELECT DISTINCT c FROM Consulta c " +
           "INNER JOIN Prontuario pr ON pr.consulta.id = c.id " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "LEFT JOIN FETCH c.paciente pac " +
           "LEFT JOIN FETCH c.especialidade " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "AND c.paciente.paciCodigo = :pacienteId " +
           "AND (:profissionalId IS NULL OR p.id = :profissionalId) " +
           "AND c.status IN ('REALIZADA', 'PAGO') " +
           "ORDER BY c.dataHora DESC")
    List<Consulta> findHistoricoCompletoPacienteMedico(
        @Param("pacienteId") Long pacienteId,
        @Param("organizacaoId") Long organizacaoId,
        @Param("profissionalId") Long profissionalId);

    /**
     * Soma o valor das consultas realizadas por organização em um período.
     * Considera REALIZADA e PAGO: o fluxo normal é REALIZADA -> PAGO e o valor não pode sumir ao quitar.
     * @return [somaValor, quantidade]
     */
    @Query("SELECT COALESCE(SUM(c.valor), 0), COUNT(c) FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.status IN ('REALIZADA', 'PAGO') " +
           "AND c.valor IS NOT NULL " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    List<Object[]> somarValorConsultasRealizadasPorOrganizacao(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    /**
     * Soma o valor das consultas realizadas agrupadas por mês/ano.
     * @return [ano, mes, somaValor, quantidade]
     */
    @Query("SELECT YEAR(c.dataHora), MONTH(c.dataHora), COALESCE(SUM(c.valor), 0), COUNT(c) " +
           "FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.status IN ('REALIZADA', 'PAGO') " +
           "AND c.valor IS NOT NULL " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "GROUP BY YEAR(c.dataHora), MONTH(c.dataHora) " +
           "ORDER BY YEAR(c.dataHora), MONTH(c.dataHora)")
    List<Object[]> somarValorConsultasAgrupadasPorMes(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    /**
     * Soma o valor das consultas realizadas agrupadas por semana ISO.
     * @return [ano, semana, somaValor, quantidade]
     */
    @Query("SELECT YEAR(c.dataHora), WEEK(c.dataHora), COALESCE(SUM(c.valor), 0), COUNT(c) " +
           "FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.status IN ('REALIZADA', 'PAGO') " +
           "AND c.valor IS NOT NULL " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "GROUP BY YEAR(c.dataHora), WEEK(c.dataHora) " +
           "ORDER BY YEAR(c.dataHora), WEEK(c.dataHora)")
    List<Object[]> somarValorConsultasAgrupadasPorSemana(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    // ===== Dashboard da plataforma (SUPER_ADMIN): visão global, sem filtro de tenant =====

    /** Consultas em [inicio, fim) de todas as clínicas, exceto canceladas. */
    @Query("SELECT COUNT(c) FROM Consulta c " +
           "WHERE c.dataHora >= :inicio AND c.dataHora < :fim AND c.status <> 'CANCELADA'")
    long contarNaoCanceladasNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** Consultas por mês em [inicio, fim), exceto canceladas. */
    @Query("SELECT YEAR(c.dataHora) AS ano, MONTH(c.dataHora) AS mes, COUNT(c) AS quantidade " +
           "FROM Consulta c " +
           "WHERE c.dataHora >= :inicio AND c.dataHora < :fim AND c.status <> 'CANCELADA' " +
           "GROUP BY YEAR(c.dataHora), MONTH(c.dataHora)")
    List<PlataformaProjecoes.ContagemMensal> contarNaoCanceladasPorMes(@Param("inicio") LocalDateTime inicio,
                                                                       @Param("fim") LocalDateTime fim);

    /**
     * Clínicas ativas (assinatura vigente em TRIAL, ATIVA ou INADIMPLENTE) com mais consultas em
     * [inicio, fim), exceto canceladas. O limite vem do Pageable.
     */
    @Query("SELECT o.id AS organizacaoId, o.nome AS nome, p.tipo AS plano, a.status AS status, " +
           "COUNT(c) AS consultasMes " +
           "FROM Consulta c JOIN c.organizacao o " +
           "JOIN AssinaturaTenant a ON a.organizacao.id = o.id JOIN a.planoAssinatura p " +
           "WHERE c.dataHora >= :inicio AND c.dataHora < :fim AND c.status <> 'CANCELADA' " +
           "AND a.status IN ('TRIAL', 'ATIVA', 'INADIMPLENTE') AND " + AssinaturaTenantRepository.VIGENTE +
           " GROUP BY o.id, o.nome, p.tipo, a.status ORDER BY COUNT(c) DESC, o.id ASC")
    List<PlataformaProjecoes.ConsultasPorClinica> buscarTopClinicasPorConsultas(
            @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, Pageable pageable);
}
