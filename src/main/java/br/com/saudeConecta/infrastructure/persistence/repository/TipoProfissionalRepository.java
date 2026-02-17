package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.profissional.TipoProfissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoProfissionalRepository extends JpaRepository<TipoProfissional, Long> {
    
    Optional<TipoProfissional> findByCodigo(String codigo);
    
    List<TipoProfissional> findByStatus(Byte status);
    
    List<TipoProfissional> findByStatusOrderByNomeAsc(Byte status);
    
    boolean existsByCodigo(String codigo);
}
