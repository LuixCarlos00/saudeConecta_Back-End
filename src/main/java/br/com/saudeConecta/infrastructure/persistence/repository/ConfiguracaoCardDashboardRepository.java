package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.dashboard.ConfiguracaoCardDashboard;
import br.com.saudeConecta.domain.dashboard.TipoCardDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoCardDashboardRepository extends JpaRepository<ConfiguracaoCardDashboard, Long> {

    // ── Por usuário ──────────────────────────────────────────────────────────

    List<ConfiguracaoCardDashboard> findByUsuarioIdOrderByOrdemExibicaoAsc(Long usuarioId);

    List<ConfiguracaoCardDashboard> findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(Long usuarioId);

    Optional<ConfiguracaoCardDashboard> findByUsuarioIdAndTipoCard(Long usuarioId, TipoCardDashboard tipoCard);

    boolean existsByUsuarioIdAndTipoCard(Long usuarioId, TipoCardDashboard tipoCard);

    // ── Por organização ──────────────────────────────────────────────────────

    List<ConfiguracaoCardDashboard> findByOrganizacaoIdOrderByOrdemExibicaoAsc(Long organizacaoId);
}
