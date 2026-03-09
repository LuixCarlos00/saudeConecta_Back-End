package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.historicodadospessoais.HistoricoDadosPessoais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoDadosPessoaisRepository extends JpaRepository<HistoricoDadosPessoais, Long> {

    List<HistoricoDadosPessoais> findByOrganizacao_IdAndUsuario_IdOrderByCriadoEmDesc(
            Long organizacaoId, Long usuarioId);
}