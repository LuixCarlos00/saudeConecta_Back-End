package br.com.saudeConecta.application.port.out.paciente;

import br.com.saudeConecta.domain.paciente.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PacienteOutputPort {
    
    Optional<Paciente> findById(Long id);
    
    Optional<Paciente> findByPaciEmail(String email);
    
    List<Paciente> findAll();
    
    Page<Paciente> findAll(Pageable pageable);
    
    List<Paciente> findByPaciCpfContainingIgnoreCase(String cpf);
    
    List<Paciente> findByPaciRgContainingIgnoreCase(String rg);
    
    List<Paciente> findByPaciTelefoneContainingIgnoreCase(String telefone);
    
    List<Paciente> findByPaciNomeContainingIgnoreCase(String nome);
    
    Paciente save(Paciente paciente);
    
    void deleteById(Long id);
    
    boolean existsById(Long id);
}
