package br.com.saudeConecta.endpoinst.prontuario.Repository;

import br.com.saudeConecta.endpoinst.prontuario.Entity.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario,Long> {

    Prontuario findByProntCodigoConsulta_ConSttCodigoConsulata(Long ConSttCodigoConsulata);
}
