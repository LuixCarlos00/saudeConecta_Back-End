package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.prontuario.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario, Long> {

    Prontuario findByConsulta_Id(Long consultaId);
    
    List<Prontuario> findByConsulta_Paciente_PaciCodigo(Long paciCodigo);
    
    List<Prontuario> findByProfissional_Id(Long profissionalId);
    
    @Query("SELECT p FROM Prontuario p " +
           "LEFT JOIN FETCH p.profissional " +
           "LEFT JOIN FETCH p.consulta c " +
           "LEFT JOIN FETCH c.paciente " +
           "LEFT JOIN FETCH c.formaPagamento " +
           "WHERE p.consulta.id = :consultaId")
    Prontuario findByConsulta_IdWithFetch(Long consultaId);
}
