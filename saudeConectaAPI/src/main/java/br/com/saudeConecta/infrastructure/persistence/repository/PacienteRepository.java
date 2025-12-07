package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    
    Optional<Paciente> findByPaciEmail(String email);
    
    List<Paciente> findByPaciCpfContainingIgnoreCase(String cpf);
    
    List<Paciente> findByPaciRgContainingIgnoreCase(String rg);
    
    List<Paciente> findByPaciTelefoneContainingIgnoreCase(String telefone);
    
    List<Paciente> findByPaciNomeContainingIgnoreCase(String nome);
}
