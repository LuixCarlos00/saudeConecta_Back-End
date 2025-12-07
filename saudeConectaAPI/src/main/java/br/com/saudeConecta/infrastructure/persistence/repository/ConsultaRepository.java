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
}
