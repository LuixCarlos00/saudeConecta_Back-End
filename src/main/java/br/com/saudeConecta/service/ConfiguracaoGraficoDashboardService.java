package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.dashboard.ConfiguracaoGraficoDashboard;
import br.com.saudeConecta.domain.dashboard.TipoGraficoDashboard;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracaoGraficoDashboardRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.dashboard.AtualizarConfiguracaoGraficoRequest;
import br.com.saudeConecta.presentation.dto.dashboard.ConfiguracaoGraficoResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoGraficoDashboardService {

    private final ConfiguracaoGraficoDashboardRepository configuracaoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Ordem dos gráficos no dashboard administrativo:
     * 1. Consultas por Período (canto superior esquerdo)
     * 2. Agendamentos (canto superior direito)
     * 3. Saldo Financeiro (canto inferior esquerdo)
     * 4. Clínico por Especialidade (canto inferior direito)
     */
    private static final List<TipoGraficoDashboard> GRAFICOS_ADMIN_ORDENADOS = List.of(
            TipoGraficoDashboard.CONSULTAS_POR_PERIODO,
            TipoGraficoDashboard.AGENDAMENTOS_DIAS_SEMANA,
            TipoGraficoDashboard.SALDO_FINANCEIRO,
            TipoGraficoDashboard.MEDICOS_POR_ESPECIALIDADE
    );
    private static final Set<TipoGraficoDashboard> GRAFICOS_ADMIN =
            new LinkedHashSet<>(GRAFICOS_ADMIN_ORDENADOS);

    /**
     * Ordem dos gráficos no dashboard profissional:
     * 1. Média de Tempo de Consulta (canto superior esquerdo)
     * 2. Meus Agendamentos por Período (canto superior direito)
     */
    private static final List<TipoGraficoDashboard> GRAFICOS_PROFISSIONAL_ORDENADOS = List.of(
            TipoGraficoDashboard.MEDIA_TEMPO_CONSULTA,
            TipoGraficoDashboard.AGENDAMENTOS_MEDICO_PERIODO
    );
    private static final Set<TipoGraficoDashboard> GRAFICOS_PROFISSIONAL =
            new LinkedHashSet<>(GRAFICOS_PROFISSIONAL_ORDENADOS);

    /**
     * Recepcionista vê apenas:
     * 1. Consultas por Período (canto superior esquerdo)
     * 2. Agendamentos (canto superior direito)
     */
    private static final List<TipoGraficoDashboard> GRAFICOS_RECEPCIONISTA_ORDENADOS = List.of(
            TipoGraficoDashboard.CONSULTAS_POR_PERIODO,
            TipoGraficoDashboard.AGENDAMENTOS_DIAS_SEMANA
    );
    private static final Set<TipoGraficoDashboard> GRAFICOS_RECEPCIONISTA =
            new LinkedHashSet<>(GRAFICOS_RECEPCIONISTA_ORDENADOS);

    // ── Listagem ─────────────────────────────────────────────────────────────

    public List<ConfiguracaoGraficoResponse> listarConfiguracoes(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Set<TipoGraficoDashboard> tiposPermitidos = resolverTiposPermitidos(
                usuario != null ? usuario.getTipoUsuarioNovo() : null);

        return configuracaoRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(usuarioId)
                .stream()
                .filter(c -> tiposPermitidos.contains(c.getTipoGrafico()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ConfiguracaoGraficoResponse> listarGraficosAtivos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Set<TipoGraficoDashboard> tiposPermitidos = resolverTiposPermitidos(
                usuario != null ? usuario.getTipoUsuarioNovo() : null);

        var todasConfigs = configuracaoRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(usuarioId)
                .stream()
                .filter(c -> tiposPermitidos.contains(c.getTipoGrafico()))
                .collect(Collectors.toList());

        if (todasConfigs.isEmpty()) {
            log.info("Usuário {} sem configurações de gráfico — inicializando automaticamente", usuarioId);
            if (usuario != null) {
                inicializarParaNovoUsuario(usuario);
            }
            return configuracaoRepository.findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(usuarioId)
                    .stream()
                    .filter(c -> tiposPermitidos.contains(c.getTipoGrafico()))
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return todasConfigs.stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Atualização ──────────────────────────────────────────────────────────

    @Transactional
    public ConfiguracaoGraficoResponse atualizarConfiguracao(Long id,
                                                             AtualizarConfiguracaoGraficoRequest request, Long usuarioId) {

        ConfiguracaoGraficoDashboard config = configuracaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada"));

        if (!config.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Configuração não pertence ao usuário atual");
        }
        config.setAtivo(request.ativo());
        if (request.ordemExibicao() != null) {
            config.setOrdemExibicao(request.ordemExibicao());
        }
        return toResponse(configuracaoRepository.save(config));
    }

    @Transactional
    public List<ConfiguracaoGraficoResponse> atualizarMultiplasConfiguracoes(
            List<AtualizarConfiguracaoGraficoRequest> requests, Long usuarioId) {

        return requests.stream().map(request -> {
            ConfiguracaoGraficoDashboard config = configuracaoRepository
                    .findByUsuarioIdAndTipoGrafico(usuarioId, request.tipoGrafico())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Configuração não encontrada para tipo: " + request.tipoGrafico()));
            config.setAtivo(request.ativo());
            if (request.ordemExibicao() != null) {
                config.setOrdemExibicao(request.ordemExibicao());
            }
            return toResponse(configuracaoRepository.save(config));
        }).collect(Collectors.toList());
    }

    // ── Reset / inicialização ────────────────────────────────────────────────

    @Transactional
    public void resetarConfiguracoesParaPadrao(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        List<TipoGraficoDashboard> tiposOrdenados = resolverTiposOrdenados(
                usuario != null ? usuario.getTipoUsuarioNovo() : null);

        List<ConfiguracaoGraficoDashboard> configs =
                configuracaoRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(usuarioId);

        // Reordenar conforme a ordem padrão do dashboard
        int ordem = 1;
        for (TipoGraficoDashboard tipo : tiposOrdenados) {
            for (ConfiguracaoGraficoDashboard config : configs) {
                if (config.getTipoGrafico() == tipo) {
                    config.setAtivo(true);
                    config.setOrdemExibicao(ordem++);
                    configuracaoRepository.save(config);
                    break;
                }
            }
        }
    }

    @Transactional
    public List<ConfiguracaoGraficoResponse> inicializarConfiguracoesPrimeiroAcesso(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        List<TipoGraficoDashboard> tipos = resolverTiposOrdenados(usuario.getTipoUsuarioNovo());

        int ordem = 1;
        for (TipoGraficoDashboard tipo : tipos) {
            if (!configuracaoRepository.existsByUsuarioIdAndTipoGrafico(usuarioId, tipo)) {
                configuracaoRepository.save(ConfiguracaoGraficoDashboard.builder()
                        .organizacao(usuario.getOrganizacao())
                        .usuario(usuario)
                        .tipoGrafico(tipo)
                        .ativo(false)
                        .ordemExibicao(ordem++)
                        .build());
            } else {
                ordem++;
            }
        }
        return listarConfiguracoes(usuarioId);
    }

    /** Chamado ao criar um novo usuário — inicializa configs já ativas */
    @Transactional
    public void inicializarParaNovoUsuario(Usuario usuario) {
        List<TipoGraficoDashboard> tipos = resolverTiposOrdenados(usuario.getTipoUsuarioNovo());
        int ordem = 1;
        for (TipoGraficoDashboard tipo : tipos) {
            configuracaoRepository.save(ConfiguracaoGraficoDashboard.builder()
                    .organizacao(usuario.getOrganizacao())
                    .usuario(usuario)
                    .tipoGrafico(tipo)
                    .ativo(true)
                    .ordemExibicao(ordem++)
                    .build());
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    public Set<TipoGraficoDashboard> resolverTiposPermitidos(TipoUsuarioNovo tipo) {
        if (tipo == null) {
            return GRAFICOS_ADMIN;
        }
        return switch (tipo) {
            case PROFISSIONAL   -> GRAFICOS_PROFISSIONAL;
            case RECEPCIONISTA  -> GRAFICOS_RECEPCIONISTA;
            default             -> GRAFICOS_ADMIN;
        };
    }

    /**
     * Retorna a lista ordenada dos tipos de gráficos conforme o layout do dashboard.
     * @param tipo tipo de usuário
     * @return lista ordenada de tipos de gráficos
     */
    public List<TipoGraficoDashboard> resolverTiposOrdenados(TipoUsuarioNovo tipo) {
        if (tipo == null) {
            return GRAFICOS_ADMIN_ORDENADOS;
        }
        return switch (tipo) {
            case PROFISSIONAL   -> GRAFICOS_PROFISSIONAL_ORDENADOS;
            case RECEPCIONISTA  -> GRAFICOS_RECEPCIONISTA_ORDENADOS;
            default             -> GRAFICOS_ADMIN_ORDENADOS;
        };
    }

    private ConfiguracaoGraficoResponse toResponse(ConfiguracaoGraficoDashboard config) {
        return new ConfiguracaoGraficoResponse(
                config.getId(),
                config.getTipoGrafico(),
                config.getTipoGrafico().getDescricao(),
                config.getAtivo(),
                config.getOrdemExibicao()
        );
    }
}