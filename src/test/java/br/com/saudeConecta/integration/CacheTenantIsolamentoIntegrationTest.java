package br.com.saudeConecta.integration;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TermoAutorizacaoRepository;
import br.com.saudeConecta.service.CacheEvictionService;
import br.com.saudeConecta.service.ProntuarioService;
import br.com.saudeConecta.service.TermoAutorizacaoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Garante que os caches indexados por consulta (prontuario-recente e questionario-saude)
 * usam a organização do token na chave: uma clínica nunca recebe o valor que outra
 * clínica colocou em cache para o mesmo ID de consulta.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Cache por consulta - isolamento entre clínicas")
class CacheTenantIsolamentoIntegrationTest {

    private static final Long CONSULTA = 10L;

    @MockBean private ProntuarioRepository prontuarioRepository;
    @MockBean private TermoAutorizacaoRepository termoRepository;

    @Autowired private ProntuarioService prontuarioService;
    @Autowired private TermoAutorizacaoService termoService;
    @Autowired private CacheEvictionService cacheEvictionService;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void limparCaches() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    @AfterEach
    void limparTenant() {
        TenantContext.clear();
    }

    private Prontuario prontuario() {
        Prontuario p = new Prontuario();
        p.setProntCodigoProntuario(1L);
        Consulta c = new Consulta();
        c.setId(CONSULTA);
        p.setConsulta(c);
        return p;
    }

    @Test
    @DisplayName("prontuario-recente: valor em cache da clínica 1 não vaza para a clínica 2")
    void prontuarioRecenteNaoVaza() {
        when(prontuarioRepository.findIdMaisRecentePorConsulta(CONSULTA, 1L)).thenReturn(Optional.of(1L));
        when(prontuarioRepository.findByIdWithFullFetch(1L)).thenReturn(Optional.of(prontuario()));

        TenantContext.setCurrentTenant(1L);
        assertThat(prontuarioService.buscarMaisRecentePorConsulta(CONSULTA)).isNotNull();

        TenantContext.setCurrentTenant(2L);
        assertThatThrownBy(() -> prontuarioService.buscarMaisRecentePorConsulta(CONSULTA))
                .isInstanceOf(EntityNotFoundException.class);
        verify(prontuarioRepository).findIdMaisRecentePorConsulta(CONSULTA, 2L);
    }

    @Test
    @DisplayName("prontuario-recente: evicção pelo CacheEvictionService usa a mesma chave do @Cacheable")
    void prontuarioRecenteEviccao() {
        when(prontuarioRepository.findIdMaisRecentePorConsulta(CONSULTA, 1L)).thenReturn(Optional.of(1L));
        when(prontuarioRepository.findByIdWithFullFetch(1L)).thenReturn(Optional.of(prontuario()));
        TenantContext.setCurrentTenant(1L);

        prontuarioService.buscarMaisRecentePorConsulta(CONSULTA);
        prontuarioService.buscarMaisRecentePorConsulta(CONSULTA);
        verify(prontuarioRepository, times(1)).findIdMaisRecentePorConsulta(CONSULTA, 1L);

        cacheEvictionService.evictProntuarioRecente(1L, CONSULTA);
        prontuarioService.buscarMaisRecentePorConsulta(CONSULTA);
        verify(prontuarioRepository, times(2)).findIdMaisRecentePorConsulta(CONSULTA, 1L);
    }

    @Test
    @DisplayName("questionario-saude: valor em cache da clínica 1 não vaza para a clínica 2")
    void questionarioNaoVaza() {
        when(termoRepository.findByConsultaIdAndOrganizacaoId(CONSULTA, 1L))
                .thenReturn(Optional.of(TermoAutorizacao.builder().token("t").build()));

        TenantContext.setCurrentTenant(1L);
        assertThat(termoService.buscarPorConsultaId(CONSULTA)).isNotNull();

        TenantContext.setCurrentTenant(2L);
        assertThat(termoService.buscarPorConsultaId(CONSULTA)).isNull();
        verify(termoRepository).findByConsultaIdAndOrganizacaoId(CONSULTA, 2L);
    }

    @Test
    @DisplayName("questionario-saude: evicção pelo CacheEvictionService usa a mesma chave do @Cacheable")
    void questionarioEviccao() {
        when(termoRepository.findByConsultaIdAndOrganizacaoId(CONSULTA, 1L))
                .thenReturn(Optional.of(TermoAutorizacao.builder().token("t").build()));
        TenantContext.setCurrentTenant(1L);

        termoService.buscarPorConsultaId(CONSULTA);
        termoService.buscarPorConsultaId(CONSULTA);
        verify(termoRepository, times(1)).findByConsultaIdAndOrganizacaoId(CONSULTA, 1L);

        cacheEvictionService.evictQuestionarioSaude(1L, CONSULTA);
        termoService.buscarPorConsultaId(CONSULTA);
        verify(termoRepository, times(2)).findByConsultaIdAndOrganizacaoId(CONSULTA, 1L);
    }
}
