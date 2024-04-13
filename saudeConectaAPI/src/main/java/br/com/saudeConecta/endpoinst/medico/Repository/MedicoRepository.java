package br.com.saudeConecta.endpoinst.medico.Repository;

import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicoRepository  extends JpaRepository<Medico,Long> {


    Optional<Medico> findByMedEmail(String MedEmail);
}
