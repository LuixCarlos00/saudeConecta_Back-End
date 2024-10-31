package br.com.saudeConecta.endpoinst.consultaStatus.Repository;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaStatusRepository extends JpaRepository<ConsultaStatus, Long> {


    boolean existsByConSttHorarioAndConSttDataAndConSttMedico_MedCodigo(String conSttHorario, String conSttData, Long medCodigo);

    Optional<ConsultaStatus> findByConSttMedico_MedCodigoAndConSttPaciente_PaciCodigoAndConSttHorarioAndConSttDataAndConSttDataCriacaoAndConSttAdm(Long medCodigo, Long paciCodigo, String conSttHorario, String conSttData, String ConSttDataCriacao, Long ConSttAdm);

    @Query("select c from ConsultaStatus c where c.conSttMedico.medCodigo = ?1")
    List<ConsultaStatus> findByConSttMedico_MedCodigo(Long medCodigo);


    List<ConsultaStatus> findByConSttPaciente_PaciCodigo(Long paciCodigo);


    @Query("select c from ConsultaStatus c where c.conSttData between :dataInicial and :dataFinal")
    List<ConsultaStatus> BuscandoTodasConsultas_CONCLUIDADAS_EmIntervaloDeDatas(@Param("dataInicial") String dataInicial, @Param("dataFinal") String dataFinal);


}