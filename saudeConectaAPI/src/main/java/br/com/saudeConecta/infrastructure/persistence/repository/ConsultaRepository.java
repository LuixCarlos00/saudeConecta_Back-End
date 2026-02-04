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
    
    List<Consulta> findByOrganizacao_IdAndProfissional_Id(Long organizacaoId, Long profissionalId);
    
    List<Consulta> findByOrganizacao_IdAndPaciente_PaciCodigo(Long organizacaoId, Long pacienteId);
    
    List<Consulta> findByOrganizacao_IdAndStatus(Long organizacaoId, StatusConsulta status);
    
    @Query("SELECT c FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND c.dataHora BETWEEN :inicio AND :fim ORDER BY c.dataHora")
    List<Consulta> findByOrganizacaoIdAndPeriodo(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);
    
    @Query("SELECT c FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profId AND DATE(c.dataHora) = :data ORDER BY c.dataHora")
    List<Consulta> findAgendaDia(
        @Param("orgId") Long organizacaoId,
        @Param("profId") Long profissionalId,
        @Param("data") LocalDate data);
    
    @Query("SELECT c FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND DATE(c.dataHora) = CURRENT_DATE ORDER BY c.dataHora")
    List<Consulta> findConsultasHoje(@Param("orgId") Long organizacaoId);
    
    @Query("SELECT c FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND c.profissional.id = :profId " +
           "AND c.dataHora BETWEEN :inicio AND :fim " +
           "AND c.status NOT IN ('CANCELADA', 'NAO_COMPARECEU')")
    List<Consulta> findConsultasAtivasProfissionalPeriodo(
        @Param("orgId") Long organizacaoId,
        @Param("profId") Long profissionalId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);
    
    boolean existsByProfissional_IdAndDataHoraAndStatusNot(
        Long profissionalId, LocalDateTime dataHora, StatusConsulta status);
    
    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND DATE(c.dataHora) = CURRENT_DATE AND c.status = 'AGENDADA'")
    Long countAgendadasHoje(@Param("orgId") Long organizacaoId);
    
    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.organizacao.id = :orgId " +
           "AND c.dataHora BETWEEN :inicio AND :fim")
    Long countByOrganizacaoIdAndPeriodo(
        @Param("orgId") Long organizacaoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim);
}
