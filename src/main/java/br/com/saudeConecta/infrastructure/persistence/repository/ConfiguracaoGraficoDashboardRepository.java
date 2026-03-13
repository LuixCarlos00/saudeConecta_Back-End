package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.dashboard.ConfiguracaoGraficoDashboard;
import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoGraficoDashboardRepository extends JpaRepository<ConfiguracaoGraficoDashboard, Long> {

    // ── Por usuário ──────────────────────────────────────────────────────────

    List<ConfiguracaoGraficoDashboard> findByUsuarioIdOrderByOrdemExibicaoAsc(Long usuarioId);

    List<ConfiguracaoGraficoDashboard> findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(Long usuarioId);

    Optional<ConfiguracaoGraficoDashboard> findByUsuarioIdAndTipoGrafico(Long usuarioId, TipoGraficoDashboard tipoGrafico);

    boolean existsByUsuarioIdAndTipoGrafico(Long usuarioId, TipoGraficoDashboard tipoGrafico);

}