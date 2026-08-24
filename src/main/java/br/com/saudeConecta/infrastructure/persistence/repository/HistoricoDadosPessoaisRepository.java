package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.historicodadospessoais.HistoricoDadosPessoais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoDadosPessoaisRepository extends JpaRepository<HistoricoDadosPessoais, Long> {

    List<HistoricoDadosPessoais> findByOrganizacao_IdAndUsuario_IdOrderByCriadoEmDesc(
            Long organizacaoId, Long usuarioId);

    List<HistoricoDadosPessoais> findByOrganizacao_IdAndEntidadeAndIdEntidadeOrderByCriadoEmDesc(
            Long organizacaoId, String entidade, Long idEntidade);

    // Métodos para SUPER_ADMIN (sem filtro de organização)
    List<HistoricoDadosPessoais> findByUsuario_IdOrderByCriadoEmDesc(Long usuarioId);

    List<HistoricoDadosPessoais> findByEntidadeAndIdEntidadeOrderByCriadoEmDesc(
            String entidade, Long idEntidade);

    // Busca os 10 registros mais recentes globalmente (para SUPER_ADMIN)
    List<HistoricoDadosPessoais> findTop10ByOrderByCriadoEmDesc();
}