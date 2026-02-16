package br.com.saudeConecta.repository;

import br.com.saudeConecta.domain.ConfiguracaoGraficoDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoGraficoDashboardRepository extends JpaRepository<ConfiguracaoGraficoDashboard, Long> {

    List<ConfiguracaoGraficoDashboard> findByOrganizacaoIdOrderByOrdemExibicaoAsc(Long organizacaoId);

    List<ConfiguracaoGraficoDashboard> findByOrganizacaoIdAndAtivoTrueOrderByOrdemExibicaoAsc(Long organizacaoId);

    Optional<ConfiguracaoGraficoDashboard> findByOrganizacaoIdAndTipoGrafico(Long organizacaoId, String tipoGrafico);

    boolean existsByOrganizacaoId(Long organizacaoId);
}
