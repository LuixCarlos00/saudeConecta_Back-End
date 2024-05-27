package br.com.saudeConecta.endpoinst.medico.Repository;

import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository  extends JpaRepository<Medico,Long> {


    Optional<Medico> findByMedEmail(String MedEmail);

    Optional<Medico> findByUsuario_Id(Long id);

    List<Medico> findByMedNomeContainingIgnoreCase(String nome);

    List<Medico> findByMedCrmContainingIgnoreCase(String crm);

    List<Medico> findByEndereco_EndMunicipioContainingIgnoreCase(String cidade);

    List<Medico> findByMedEspecialidadeContainingIgnoreCase(String especialidade);

    boolean existsByUsuario_Id(Long id);
}
