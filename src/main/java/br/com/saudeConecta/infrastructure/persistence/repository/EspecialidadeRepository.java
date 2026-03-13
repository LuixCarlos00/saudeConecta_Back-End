package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.profissional.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {
    
    List<Especialidade> findByTipoProfissional_IdAndStatus(Long tipoProfissionalId, Byte status);
    
    @Query("SELECT e FROM Especialidade e WHERE e.tipoProfissional.codigo = :codigo AND e.status = 1")
    List<Especialidade> findAtivasByTipoProfissionalCodigo(@Param("codigo") String codigo);
    
    @Query("SELECT e FROM Especialidade e JOIN FETCH e.tipoProfissional WHERE e.status = 1")
    List<Especialidade> findAllAtivasWithTipoProfissional();
    
    Optional<Especialidade> findByTipoProfissional_IdAndNome(Long tipoProfissionalId, String nome);
    
    boolean existsByTipoProfissional_IdAndNome(Long tipoProfissionalId, String nome);
}
