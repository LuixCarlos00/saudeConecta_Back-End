package br.com.saudeConecta.application.dashboard;

import br.com.saudeConecta.application.dashboard.dto.ConfiguracaoGraficoResponse;
import br.com.saudeConecta.domain.dashboard.ConfiguracaoGraficoDashboard;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracaoGraficoDashboardRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoGraficoDashboardServiceTest {

    @Mock
    private ConfiguracaoGraficoDashboardRepository configuracaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private ConfiguracaoGraficoDashboardService service;

    private Usuario clinico() {
        return Usuario.builder().id(7L).tipoUsuarioNovo(TipoUsuarioNovo.CLINICO).build();
    }

    @Test
    @DisplayName("listarGraficosAtivos inicializa configs ativas quando usuário não tem nenhuma")
    void listarGraficosAtivosInicializaQuandoSemConfiguracao() {
        Usuario usuario = clinico();
        int esperados = service.resolverTiposOrdenados(TipoUsuarioNovo.CLINICO).size();
        List<ConfiguracaoGraficoDashboard> salvas = new ArrayList<>();

        when(usuarioRepository.findById(7L)).thenReturn(java.util.Optional.of(usuario));
        when(configuracaoRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(7L)).thenReturn(List.of());
        when(configuracaoRepository.save(any(ConfiguracaoGraficoDashboard.class))).thenAnswer(inv -> {
            ConfiguracaoGraficoDashboard c = inv.getArgument(0);
            salvas.add(c);
            return c;
        });
        when(configuracaoRepository.findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(7L))
                .thenReturn(List.of()) // checagem inicial
                .thenAnswer(inv -> new ArrayList<>(salvas)); // após inicializar

        List<ConfiguracaoGraficoResponse> resposta = service.listarGraficosAtivos(7L);

        ArgumentCaptor<ConfiguracaoGraficoDashboard> captor =
                ArgumentCaptor.forClass(ConfiguracaoGraficoDashboard.class);
        verify(configuracaoRepository, times(esperados)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(c -> Boolean.TRUE.equals(c.getAtivo())));
        assertEquals(esperados, resposta.size());
    }

    @Test
    @DisplayName("listarGraficosAtivos não inicializa quando já há configuração")
    void listarGraficosAtivosNaoInicializaQuandoJaExiste() {
        when(usuarioRepository.findById(7L)).thenReturn(java.util.Optional.of(clinico()));
        when(configuracaoRepository.findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(7L))
                .thenReturn(List.of());
        when(configuracaoRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(7L))
                .thenReturn(List.of(ConfiguracaoGraficoDashboard.builder().build()));

        assertTrue(service.listarGraficosAtivos(7L).isEmpty());
        verify(configuracaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("caminho interno não chama método @Transactional via this (anotação seria ignorada)")
    void caminhoInternoUsaHelperSemTransactional() throws Exception {
        Method helper = ConfiguracaoGraficoDashboardService.class
                .getDeclaredMethod("criarConfiguracoesPadrao", Usuario.class);
        assertFalse(helper.isAnnotationPresent(Transactional.class));
        assertTrue(ConfiguracaoGraficoDashboardService.class
                .getMethod("inicializarParaNovoUsuario", Usuario.class)
                .isAnnotationPresent(Transactional.class));
    }
}
