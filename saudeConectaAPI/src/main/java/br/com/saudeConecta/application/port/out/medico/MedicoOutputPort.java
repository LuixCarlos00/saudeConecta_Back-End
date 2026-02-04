package br.com.saudeConecta.application.port.out.medico;

import br.com.saudeConecta.domain.medico.Medico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MedicoOutputPort {
    
    Optional<Medico> findById(Long id);
    
    Optional<Medico> findByUsuario_Id(Long usuarioId);
    
    Optional<Medico> buscarMedicoPorIdUsuario(Long usuarioId);
    
    Optional<Medico> findByMedEmail(String email);
    
    List<Medico> findAll();
    
    Page<Medico> findAll(Pageable pageable);
    
    List<Medico> findByMedCrmContainingIgnoreCase(String crm);
    
    List<Medico> findByMedNomeContainingIgnoreCase(String nome);
    
    List<Medico> findByMedEspecialidadeContainingIgnoreCase(String especialidade);
    
    List<Medico> findByEndereco_EndMunicipioContainingIgnoreCase(String municipio);
    
    Long contarMedicosAtivos();
    
    Medico save(Medico medico);
    
    void deleteById(Long id);
    
    boolean existsById(Long id);
    
    boolean existsByUsuario_Id(Long usuarioId);

    List<Medico> findAllWithUsuario();
}
