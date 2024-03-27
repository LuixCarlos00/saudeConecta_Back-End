package br.com.saudeConecta.endpoinst.paciente.Repository;

import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente,Long> {
}
