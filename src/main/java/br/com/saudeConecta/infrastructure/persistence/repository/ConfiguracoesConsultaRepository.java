package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.consulta.ConfiguracoesConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracoesConsultaRepository extends JpaRepository<ConfiguracoesConsulta, Long> {

    Optional<ConfiguracoesConsulta> findByOrganizacaoId(Long organizacaoId);

    boolean existsByOrganizacaoId(Long organizacaoId);
}
