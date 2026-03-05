package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.planos.PlanoAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanoAssinaturaRepository extends JpaRepository<PlanoAssinatura, Long> {

    List<PlanoAssinatura> findByAtivoTrue();

    Optional<PlanoAssinatura> findByTipo(TipoPlano tipo);

    Optional<PlanoAssinatura> findByIdAndAtivoTrue(Long id);

    boolean existsByTipo(TipoPlano tipo);
}
