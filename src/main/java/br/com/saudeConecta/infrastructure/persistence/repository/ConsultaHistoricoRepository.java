package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.consulta.ConsultaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultaHistoricoRepository extends JpaRepository<ConsultaHistorico, Long> {
    
    List<ConsultaHistorico> findByConsulta_IdOrderByCreatedAtDesc(Long consultaId);
}
