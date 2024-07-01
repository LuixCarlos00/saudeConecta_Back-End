package br.com.saudeConecta.endpoinst.consulta.Repository;

import br.com.saudeConecta.endpoinst.consulta.Entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta,Long> {


    Optional<Consulta> findByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, Date conData, Long medCodigo);

    boolean existsByConHorarioAndConDataAndConMedico_MedCodigo(String conHorario, String conData, Long medCodigo);

    List<Consulta> findByConMedico_MedCodigoAndConData(Long medCodigo, String conData);
}