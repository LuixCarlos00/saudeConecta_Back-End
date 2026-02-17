package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario, Long> {

    Prontuario findByConsulta_Id(Long consultaId);
    
    List<Prontuario> findByConsulta_Paciente_PaciCodigo(Long paciCodigo);
    
    List<Prontuario> findByProfissional_Id(Long profissionalId);
}
