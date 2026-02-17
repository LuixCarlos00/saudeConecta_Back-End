package br.com.saudeConecta.infrastructure.persistence.repository;

import br.com.saudeConecta.domain.dashboard.ConfiguracaoGraficoDashboard;
import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoGraficoDashboardRepository extends JpaRepository<ConfiguracaoGraficoDashboard, Long> {
    
    List<ConfiguracaoGraficoDashboard> findByOrganizacaoIdOrderByOrdemExibicaoAsc(Long organizacaoId);
    
    List<ConfiguracaoGraficoDashboard> findByOrganizacaoIdAndAtivoTrueOrderByOrdemExibicaoAsc(Long organizacaoId);
    
    Optional<ConfiguracaoGraficoDashboard> findByOrganizacaoIdAndTipoGrafico(Long organizacaoId, TipoGraficoDashboard tipoGrafico);
    
    @Query("SELECT c FROM ConfiguracaoGraficoDashboard c WHERE c.organizacao.id = :organizacaoId AND c.ativo = true ORDER BY c.ordemExibicao ASC")
    List<ConfiguracaoGraficoDashboard> findGraficosAtivos(@Param("organizacaoId") Long organizacaoId);
    
    boolean existsByOrganizacaoIdAndTipoGrafico(Long organizacaoId, TipoGraficoDashboard tipoGrafico);
}
