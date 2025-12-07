package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario,Long> {

    Prontuario findByProntCodigoConsulta_ConSttCodigoConsulata(Long ConSttCodigoConsulata);
}
