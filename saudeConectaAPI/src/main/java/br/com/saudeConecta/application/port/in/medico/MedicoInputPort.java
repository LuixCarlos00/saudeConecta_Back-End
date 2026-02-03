package br.com.saudeConecta.application.port.in.medico;

import br.com.saudeConecta.domain.medico.Medico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MedicoInputPort {
    
    Optional<Medico> buscarPorId(Long id);
    
    Optional<Medico> buscarPorIdUsuario(Long usuarioId);
    
    Optional<Medico> buscarPorEmail(String email);
    
    List<Medico> buscarTodos();
    
    Page<Medico> buscarTodos(Pageable pageable);
    
    List<Medico> buscarPorCrm(String crm);
    
    List<Medico> buscarPorNome(String nome);
    
    List<Medico> buscarPorEspecialidade(String especialidade);
    
    List<Medico> buscarPorMunicipio(String municipio);
    
    Long contarMedicosAtivos();
    
    Medico cadastrar(Medico medico);
    
    void deletar(Long id) throws Exception;
}
