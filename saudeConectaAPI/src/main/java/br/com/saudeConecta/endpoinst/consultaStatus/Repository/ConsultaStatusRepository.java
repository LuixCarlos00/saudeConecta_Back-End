package br.com.saudeConecta.endpoinst.consultaStatus.Repository;

import br.com.saudeConecta.endpoinst.consultaStatus.Entity.ConsultaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaStatusRepository extends JpaRepository<ConsultaStatus,Long> {


    //Optional<ConsultaStatus> findByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, Date conData, Long medCodigo);

   // boolean existsByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, String conData, Long medCodigo);
   boolean existsByConSttHorarioAndConSttDataAndConSttMedico_MedCodigo(String conSttHorario, String conSttData, Long medCodigo);

    Optional<ConsultaStatus> findByConSttMedico_MedCodigoAndConSttPaciente_PaciCodigoAndConSttHorarioAndConSttDataAndConSttDataCriacaoAndConSttAdm(Long medCodigo, Long paciCodigo, String conSttHorario, String conSttData, String ConSttDataCriacao, Long ConSttAdm);

    @Query("select c from ConsultaStatus c where c.conSttMedico.medCodigo = ?1")
    List<ConsultaStatus> findByConSttMedico_MedCodigo(Long medCodigo);
}