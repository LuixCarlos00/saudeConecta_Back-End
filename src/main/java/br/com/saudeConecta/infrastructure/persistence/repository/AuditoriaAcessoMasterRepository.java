package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.usuario.AuditoriaAcessoMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaAcessoMasterRepository extends JpaRepository<AuditoriaAcessoMaster, Long> {
}
