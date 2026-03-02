package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    
    List<Consulta> findByOrganizacao_Id(Long organizacaoId);
    
    Page<Consulta> findByOrganizacao_Id(Long organizacaoId, Pageable pageable);
    
    Optional<Consulta> findByIdAndOrganizacao_Id(Long id, Long organizacaoId);
    
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
           "WHERE c.organizacao.id = :organizacaoId AND c.profissional.id = :profissionalId")
    List<Consulta> findByOrganizacao_IdAndProfissional_IdWithRelations(
        @Param("organizacaoId") Long organizacaoId,
        @Param("profissionalId") Long profissionalId);
    
    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :organizacaoId AND c.paciente.paciCodigo = :pacienteId")
    List<Consulta> findByOrganizacao_IdAndPaciente_PaciCodigoWithRelations(
        @Param("organizacaoId") Long organizacaoId,
        @Param("pacienteId") Long pacienteId);
    
    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :organizacaoId")
    List<Consulta> findByOrganizacao_IdWithRelations(@Param("organizacaoId") Long organizacaoId);
    

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
           "AND c.profissional.id = :profId AND DATE(c.dataHora) = :data ORDER BY c.dataHora")
    List<Consulta> findAgendaDia(
        @Param("orgId") Long organizacaoId,
        @Param("profId") Long profissionalId,
        @Param("data") LocalDate data);
    
    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND DATE(c.dataHora) = CURRENT_DATE ORDER BY c.dataHora")
    List<Consulta> findConsultasHoje(@Param("orgId") Long organizacaoId);
    
//    @Query("SELECT c FROM Consulta c WHERE c.organizacao.id = :orgId " +
//           "AND c.profissional.id = :profId " +
//           "AND c.dataHora BETWEEN :inicio AND :fim " +
//           "AND c.status NOT IN ('CANCELADA')")
//    List<Consulta> findConsultasAtivasProfissionalPeriodo(
//        @Param("orgId") Long organizacaoId,
//        @Param("profId") Long profissionalId,
//        @Param("inicio") LocalDateTime inicio,
//        @Param("fim") LocalDateTime fim);


    boolean existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
        Long profissionalId, Long organizacaoId, LocalDateTime dataHora, StatusConsulta status);


    
    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND DATE(c.dataHora) = CURRENT_DATE AND c.status = 'AGENDADA'")
    Long countAgendadasHoje(@Param("orgId") Long organizacaoId);
    


    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    Long countByOrganizacao_IdAndDataHoraBetween(Long organizacaoId, LocalDateTime inicio, LocalDateTime fim);

    Long countByOrganizacao_IdAndStatusAndDataHoraBetween(
        Long organizacaoId, StatusConsulta status, LocalDateTime inicio, LocalDateTime fim);



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
           "WHERE c.profissional.id = :profissionalId " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByProfissionalIdAndDataHoraBetweenWithRelations(
        @Param("profissionalId") Long profissionalId,
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
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndProfissionalIdAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("usuarioId") Long usuarioId,
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
    List<Consulta> findByOrganizacaoIdAndProfissionalIdAndStatusOptionalAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("usuarioId") Long usuarioId,
        @Param("status") StatusConsulta status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndProfissionalIdDirectAndStatusOptionalAndDataHoraBetween(
        @Param("orgId") Long orgId,
        @Param("profissionalId") Long profissionalId,
        @Param("status") StatusConsulta status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("profissionalId") Long profissionalId,
        @Param("especialidade") String especialidade,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("profissionalId") Long profissionalId,
        @Param("especialidade") String especialidade,
        @Param("status") StatusConsulta status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("especialidade") String especialidade,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
        @Param("orgId") Long orgId,
        @Param("especialidade") String especialidade,
        @Param("status") StatusConsulta status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndEspecialidadeNomeContainingWithRelations(
        @Param("orgId") Long orgId,
        @Param("especialidade") String especialidade);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
        @Param("orgId") Long orgId,
        @Param("especialidade") String especialidade,
        @Param("status") StatusConsulta status);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingWithRelations(
        @Param("orgId") Long orgId,
        @Param("profissionalId") Long profissionalId,
        @Param("especialidade") String especialidade);

    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade e " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profissionalId " +
           "AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :especialidade, '%')) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
        @Param("orgId") Long orgId,
        @Param("profissionalId") Long profissionalId,
        @Param("especialidade") String especialidade,
        @Param("status") StatusConsulta status);

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    Long countByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    Long countByStatusAndDataHoraBetween(StatusConsulta status, LocalDateTime inicio, LocalDateTime fim);


    @Query("SELECT c FROM Consulta c " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.especialidade " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE c.dataHora BETWEEN :inicio AND :fim " +
           "ORDER BY c.dataHora")
    List<Consulta> findByDataHoraBetweenWithRelations(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

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
    //Todo tirar o 'realizando'

    // ==========================================
    // ESTATÍSTICAS POR PROFISSIONAL (usuarioId + orgId) - HOJE
    // ==========================================

    @Query("SELECT COUNT(c) FROM Consulta c " +
           "JOIN c.profissional p " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    Long countConsultasHojePorUsuarioEOrg(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(c) FROM Consulta c " +
           "JOIN c.profissional p " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND c.status = br.com.saudeConecta.domain.consulta.StatusConsulta.REALIZADA " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    Long countConsultasRealizadasHojePorUsuarioEOrg(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(c) FROM Consulta c " +
           "JOIN c.profissional p " +
           "WHERE c.organizacao.id = :orgId " +
           "AND p.usuario.id = :usuarioId " +
           "AND c.status = br.com.saudeConecta.domain.consulta.StatusConsulta.AGENDADA " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    Long countConsultasAgendadasHojePorUsuarioEOrg(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

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

    @Query("SELECT COUNT(c) FROM Consulta c " +
           "LEFT JOIN c.profissional p " +
           "WHERE c.organizacao.id = :orgId " +
           "AND (:usuarioId IS NULL OR p.usuario.id = :usuarioId) " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    Long countConsultasPorMedicoEIntervalo(
        @Param("orgId") Long organizacaoId,
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(c) FROM Consulta c " +
           "WHERE c.organizacao.id = :orgId " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    Long countConsultasSemanaByOrganizacao(
        @Param("orgId") Long organizacaoId,
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

    @Query("SELECT COUNT(c) FROM Consulta c " +
           "WHERE c.dataHora BETWEEN :inicio AND :fim")
    Long countConsultasSemanaGlobal(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);

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
     * Busca histórico completo de consultas REALIZADAS de um paciente que POSSUEM prontuário
     * Usa INNER JOIN com Prontuario para garantir que apenas consultas com prontuário sejam retornadas
     * Filtra por organização e retorna apenas consultas com status REALIZADA
     * 
     * @param pacienteId ID do paciente
     * @param organizacaoId ID da organização
     * @return Lista de consultas REALIZADAS com prontuário, ordenadas por data/hora (mais recente primeiro)
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
           "AND c.status = 'REALIZADA' " +
           "ORDER BY c.dataHora DESC")
    List<Consulta> findHistoricoCompletoPaciente(
        @Param("pacienteId") Long pacienteId,
        @Param("organizacaoId") Long organizacaoId);

    /**
     * Busca histórico completo de consultas REALIZADAS de um paciente que POSSUEM prontuário odontológico
     * Usa INNER JOIN com ProntuarioDentista para garantir que apenas consultas com prontuário dental sejam retornadas
     *
     * @param pacienteId ID do paciente
     * @param organizacaoId ID da organização
     * @return Lista de consultas REALIZADAS com prontuário dentista, ordenadas por data/hora (mais recente primeiro)
     */
    @Query("SELECT DISTINCT c FROM Consulta c " +
           "INNER JOIN ProntuarioDentista pd ON pd.consulta.id = c.id " +
           "LEFT JOIN FETCH c.profissional p " +
           "LEFT JOIN FETCH p.tipoProfissional " +
           "LEFT JOIN FETCH p.especialidades " +
           "LEFT JOIN FETCH c.paciente pac " +
           "LEFT JOIN FETCH c.especialidade " +
           "WHERE c.organizacao.id = :organizacaoId " +
           "AND c.paciente.paciCodigo = :pacienteId " +
           "AND c.status = 'REALIZADA' " +
           "ORDER BY c.dataHora DESC")
    List<Consulta> findHistoricoCompletoPacienteDentista(
        @Param("pacienteId") Long pacienteId,
        @Param("organizacaoId") Long organizacaoId);
}
