package br.com.saudeConecta.endpoinst.paciente.Repository;

import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente,Long> {
    Optional<Paciente> findByPaciEmail(String PaciEmail);

    Optional<Paciente> findByUsuario_Id(Long id);
}
