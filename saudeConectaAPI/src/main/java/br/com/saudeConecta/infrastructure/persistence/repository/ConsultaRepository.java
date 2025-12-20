package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.consulta.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    Optional<Consulta> findByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, Date conData, Long medCodigo);

    boolean existsByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, String conData, Long medCodigo);

    List<Consulta> findByConMedico_MedCodigoAndConData(Long medCodigo, String conData);

    List<Consulta> findByConMedico_MedCodigo(Long medCodigo);

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal")
    List<Consulta> buscarConsultasEmIntervaloDeDatas(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal);

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conMedico.medEspecialidade = :especialidade")
    List<Consulta> buscarConsultasEmIntervaloComEspecialidade(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal,
            @Param("especialidade") String especialidade);

    @Query("SELECT c FROM Consulta c WHERE c.conMedico.medCodigo = :medicoId")
    List<Consulta> buscarConsultasPorMedico(@Param("medicoId") Long medicoId);

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conMedico.medCodigo = :medicoId")
    List<Consulta> buscarConsultasPorMedicoEmIntervalo(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal,
            @Param("medicoId") Long medicoId);

    @Query("SELECT c FROM Consulta c WHERE c.conMedico.medEspecialidade = :especialidade")
    List<Consulta> buscarConsultasPorEspecialidade(@Param("especialidade") String especialidade);

    @Query("SELECT c.conStatus, COUNT(c) FROM Consulta c WHERE c.conMedico.medCodigo = :medicoId AND c.conData BETWEEN :dataInicial AND :dataFinal GROUP BY c.conStatus")
    List<Object[]> contarConsultasPorStatusEMedico(
            @Param("medicoId") Long medicoId,
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal);

    List<Consulta> findByConPaciente_PaciCodigo(Long paciCodigo);

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal")
    List<Consulta>  buscarConsultasPorIntervaloDeDatas(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal);

    // ==========================================
    // CONSULTAS CONCLUÍDAS (REALIZADA)
    // ==========================================

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasEmIntervaloDeDatas(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal);

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conMedico.medEspecialidade = :especialidade AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasEmIntervaloComEspecialidade(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal,
            @Param("especialidade") String especialidade);

    @Query("SELECT c FROM Consulta c WHERE c.conMedico.medCodigo = :medicoId AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasPorMedico(@Param("medicoId") Long medicoId);

    @Query("SELECT c FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conMedico.medCodigo = :medicoId AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasPorMedicoEmIntervalo(
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal,
            @Param("medicoId") Long medicoId);

    @Query("SELECT c FROM Consulta c WHERE c.conMedico.medEspecialidade = :especialidade AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasPorEspecialidade(@Param("especialidade") String especialidade);

    @Query("SELECT c FROM Consulta c WHERE c.conMedico.medCodigo = :medicoId AND c.conMedico.medEspecialidade = :especialidade AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasPorMedicoEEspecialidade(
            @Param("medicoId") Long medicoId,
            @Param("especialidade") String especialidade);

    @Query("SELECT c FROM Consulta c WHERE c.conMedico.medCodigo = :medicoId AND c.conMedico.medEspecialidade = :especialidade AND c.conData BETWEEN :dataInicial AND :dataFinal AND c.conStatus = 'REALIZADA'")
    List<Consulta> buscarConsultasConcluidasPorMedicoEspecialidadeEmIntervalo(
            @Param("medicoId") Long medicoId,
            @Param("especialidade") String especialidade,
            @Param("dataInicial") String dataInicial,
            @Param("dataFinal") String dataFinal);

    // ==========================================
    // ESTATÍSTICAS DO DASHBOARD
    // ==========================================

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData = :data")
    Long contarConsultasPorData(@Param("data") String data);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData = :data AND c.conMedico.usuario.id = :usuarioId")
    Long contarConsultasPorDataEUsuario(@Param("data") String data, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData = :data AND c.conStatus = 'REALIZADA'")
    Long contarConsultasRealizadasPorData(@Param("data") String data);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData = :data AND c.conMedico.usuario.id = :usuarioId AND c.conStatus = 'REALIZADA'")
    Long contarConsultasRealizadasPorDataEUsuario(@Param("data") String data, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData = :data AND c.conStatus = 'AGENDADA'")
    Long contarConsultasAgendadasPorData(@Param("data") String data);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData = :data AND c.conMedico.usuario.id = :usuarioId AND c.conStatus = 'AGENDADA'")
    Long contarConsultasAgendadasPorDataEUsuario(@Param("data") String data, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conStatus IN ('AGENDADA', 'REALIZADA')")
    Long contarConsultasDaSemana(@Param("dataInicial") String dataInicial, @Param("dataFinal") String dataFinal);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.conData BETWEEN :dataInicial AND :dataFinal AND c.conMedico.usuario.id = :usuarioId AND c.conStatus IN ('AGENDADA', 'REALIZADA')")
    Long contarConsultasDaSemanaPorUsuario(@Param("dataInicial") String dataInicial, @Param("dataFinal") String dataFinal, @Param("usuarioId") Long usuarioId);

}

//dasbosrde e tabela de medico trazedno dados errados