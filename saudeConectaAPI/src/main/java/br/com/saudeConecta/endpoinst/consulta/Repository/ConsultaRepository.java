package br.com.saudeConecta.endpoinst.consulta.Repository;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta,Long> {


    Optional<Consulta> findByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, Date conData, Long medCodigo);

    boolean existsByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, String conData, Long medCodigo);

    List<Consulta> findByConMedico_MedCodigoAndConData(Long medCodigo, String conData);

    List<Consulta> findByConMedico_MedCodigo(Long medCodigo);


    @Query("select c from Consulta c where c.conData between :dataInicial and :dataFinal")
    List<Consulta> BuscandoTodasConsultasEmIntervaloDeDatas(@Param("dataInicial") String dataInicial, @Param("dataFinal") String dataFinal);


    @Query("select c from Consulta c where c.conData between :dataInicial and :dataFinal and c.conMedico.medEspecialidade = :especialidade")
    List<Consulta>  BuscandoTodasConsultasEmIntervaloDeDatasComEspecialidade(@Param("dataInicial") String dataInicial, @Param("dataFinal") String dataFinal, @Param("especialidade") String especialidade);


    @Query("select c from Consulta c where c.conMedico.medCodigo = :medicoID")
    List<Consulta>  BuscandoTodasConsultasPorMedico(@Param("medicoID") Long medicoID );


    @Query("select c from Consulta c where c.conData between :dataInicial and :dataFinal and c.conMedico.medCodigo = :MedicoId")
    List<Consulta>  BuscandoTodasConsultasPorMedicoEmIntervaloDeDatas(@Param("dataInicial") String dataInicial, @Param("dataFinal") String dataFinal, @Param("MedicoId") Long MedicoId);


    @Query("select c from Consulta c where  c.conMedico.medEspecialidade = :especialidade")
    List<Consulta>  BuscandoTodasConsultasPorEspecialidade(@Param("especialidade") String especialidade);


}