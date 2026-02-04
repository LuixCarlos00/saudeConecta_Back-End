package br.com.saudeConecta.application.port.in.paciente;

import br.com.saudeConecta.domain.paciente.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PacienteInputPort {
    
    Optional<Paciente> buscarPorId(Long id);
    
    Optional<Paciente> buscarPorEmail(String email);
    
    List<Paciente> buscarTodos();
    
    Page<Paciente> buscarTodos(Pageable pageable);
    
    List<Paciente> buscarPorCpf(String cpf);
    
    List<Paciente> buscarPorRg(String rg);
    
    List<Paciente> buscarPorTelefone(String telefone);
    
    List<Paciente> buscarPorNome(String nome);
    
    Paciente cadastrar(Paciente paciente);
    
    void deletar(Long id) throws Exception;
}
