package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.medico.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {
    
    Optional<Medico> findByMedEmail(String email);
    
    Optional<Medico> findByUsuario_Id(Long usuarioId);
    
    boolean existsByUsuario_Id(Long usuarioId);
    
    List<Medico> findByMedCrmContainingIgnoreCase(String crm);
    
    List<Medico> findByEndereco_EndMunicipioContainingIgnoreCase(String municipio);
    
    List<Medico> findByMedEspecialidadeContainingIgnoreCase(String especialidade);
    
    List<Medico> findByMedNomeContainingIgnoreCase(String nome);
}
