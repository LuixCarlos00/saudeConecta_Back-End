package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.dashboard.ConfiguracaoCardDashboard;
import br.com.saudeConecta.domain.dashboard.TipoCardDashboard;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracaoCardDashboardRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.dashboard.AtualizarConfiguracaoCardRequest;
import br.com.saudeConecta.presentation.dto.dashboard.ConfiguracaoCardResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoCardDashboardService {

    private final ConfiguracaoCardDashboardRepository cardRepository;
    private final UsuarioRepository usuarioRepository;

    // ── Cards por perfil ─────────────────────────────────────────────────────
    /**
     * SuperAdmin exibe os mesmos 7 cards que o AdminOrg.
     */
    private static final Set<TipoCardDashboard> CARDS_SUPER_ADMIN = Set.of(
            TipoCardDashboard.CONSULTAS_HOJE_GLOBAL,
            TipoCardDashboard.CONSULTAS_SEMANA_GLOBAL,
            TipoCardDashboard.MEDICOS_ATIVOS_GLOBAL
    ); //CARDS_ADMIN_ORG;

    /**
     * AdministradorOrg
     */
    private static final Set<TipoCardDashboard> CARDS_ADMIN_ORG = Set.of(
            TipoCardDashboard.CONSULTAS_HOJE,
            TipoCardDashboard.CONSULTAS_ATENDIDAS,
            TipoCardDashboard.CONSULTAS_AGUARDANDO,
            TipoCardDashboard.MEDICOS_ATIVOS,
            TipoCardDashboard.CONSULTAS_SEMANA,
            TipoCardDashboard.CANCELADOS_SEMANA,
            TipoCardDashboard.CONFIRMADOS_SEMANA
    );



    /**
     * Recepcionista: mesmos cards do AdminOrg, exceto Clínicos Ativos.
     */
    private static final Set<TipoCardDashboard> CARDS_RECEPCIONISTA = Set.of(
            TipoCardDashboard.CONSULTAS_HOJE,
            TipoCardDashboard.CONSULTAS_ATENDIDAS,
            TipoCardDashboard.CONSULTAS_AGUARDANDO,
            TipoCardDashboard.CONSULTAS_SEMANA,
            TipoCardDashboard.CANCELADOS_SEMANA,
            TipoCardDashboard.CONFIRMADOS_SEMANA
    );

    /** Profissional exibe apenas os 4 cards de suas próprias consultas. */
    private static final Set<TipoCardDashboard> CARDS_PROFISSIONAL = Set.of(
            TipoCardDashboard.CONSULTAS_HOJE,
            TipoCardDashboard.CONSULTAS_ATENDIDAS,
            TipoCardDashboard.CONSULTAS_AGUARDANDO,
            TipoCardDashboard.CONSULTAS_SEMANA
    );

    // ── Listagem ─────────────────────────────────────────────────────────────

    /**
     * Lista todas as configurações de cards do usuário.
     *
     * @param usuarioId ID do usuário logado
     * @return Lista de configurações de cards
     */
    @Cacheable(value = "configuracoes-cards", key = "'lista-' + #usuarioId")
    public List<ConfiguracaoCardResponse> listarConfiguracoes(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Set<TipoCardDashboard> tiposPermitidos = resolverTiposPermitidos(
                usuario != null ? usuario.getTipoUsuarioNovo() : null);

        return cardRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(usuarioId)
                .stream()
                .filter(c -> tiposPermitidos.contains(c.getTipoCard()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista apenas os cards ativos do usuário.
     * Se não existirem configurações, inicializa automaticamente.
     *
     * @param usuarioId ID do usuário logado
     * @return Lista de cards ativos
     */
    @Cacheable(value = "configuracoes-cards", key = "'ativos-' + #usuarioId")
    @Transactional
    public List<ConfiguracaoCardResponse> listarCardsAtivos(Long usuarioId) {
        List<ConfiguracaoCardDashboard> ativosNoDb =
                cardRepository.findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(usuarioId);

        if (ativosNoDb.isEmpty()) {
            log.info("Usuario {} sem configuracoes de card — inicializando automaticamente", usuarioId);
            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null) {
                inicializarParaNovoUsuario(usuario);
                Set<TipoCardDashboard> tiposPermitidos = resolverTiposPermitidos(usuario.getTipoUsuarioNovo());
                return cardRepository.findByUsuarioIdAndAtivoTrueOrderByOrdemExibicaoAsc(usuarioId)
                        .stream()
                        .filter(c -> tiposPermitidos.contains(c.getTipoCard()))
                        .map(this::toResponse)
                        .toList();
            }
            return List.of();
        }

        return ativosNoDb.stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Atualização ──────────────────────────────────────────────────────────

    /**
     * Atualiza uma configuração de card específica pelo ID.
     *
     * @param id        ID da configuração
     * @param request   Dados a atualizar (ativo, ordemExibicao)
     * @param usuarioId ID do usuário logado (validação de posse)
     * @return Configuração atualizada
     */
    @CacheEvict(value = "configuracoes-cards", key = "'ativos-' + #usuarioId")
    @Transactional
    public ConfiguracaoCardResponse atualizarConfiguracao(Long id,
                                                          AtualizarConfiguracaoCardRequest request,
                                                          Long usuarioId) {
        ConfiguracaoCardDashboard config = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Configuração de card não encontrada"));

        if (!config.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Configuração não pertence ao usuário atual");
        }
        config.setAtivo(request.ativo());
        if (request.ordemExibicao() != null) {
            config.setOrdemExibicao(request.ordemExibicao());
        }
        return toResponse(cardRepository.save(config));
    }

    /**
     * Atualiza múltiplas configurações de uma só vez.
     *
     * @param requests  Lista de atualizações
     * @param usuarioId ID do usuário logado
     * @return Lista de configurações atualizadas
     */
    @CacheEvict(value = "configuracoes-cards", key = "'ativos-' + #usuarioId")
    @Transactional
    public List<ConfiguracaoCardResponse> atualizarMultiplasConfiguracoes(
            List<AtualizarConfiguracaoCardRequest> requests, Long usuarioId) {

        return requests.stream().map(request -> {
            ConfiguracaoCardDashboard config = cardRepository
                    .findByUsuarioIdAndTipoCard(usuarioId, request.tipoCard())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Configuração não encontrada para tipo: " + request.tipoCard()));
            config.setAtivo(request.ativo());
            if (request.ordemExibicao() != null) {
                config.setOrdemExibicao(request.ordemExibicao());
            }
            return toResponse(cardRepository.save(config));
        }).collect(Collectors.toList());
    }

    // ── Reset / Inicialização ────────────────────────────────────────────────

    /**
     * Reseta todas as configurações para o padrão (todos ativos).
     *
     * @param usuarioId ID do usuário logado
     */
    @CacheEvict(value = "configuracoes-cards", key = "'ativos-' + #usuarioId")
    @Transactional
    public void resetarConfiguracoesParaPadrao(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Set<TipoCardDashboard> tiposPermitidos = resolverTiposPermitidos(
                usuario != null ? usuario.getTipoUsuarioNovo() : null);

        List<ConfiguracaoCardDashboard> configs =
                cardRepository.findByUsuarioIdOrderByOrdemExibicaoAsc(usuarioId)
                        .stream()
                        .filter(c -> tiposPermitidos.contains(c.getTipoCard()))
                        .collect(Collectors.toList());
        int ordem = 1;
        for (ConfiguracaoCardDashboard config : configs) {
            config.setAtivo(true);
            config.setOrdemExibicao(ordem++);
            cardRepository.save(config);
        }
    }

    /**
     * Inicializa as configurações de cards no primeiro acesso do usuário.
     *
     * @param usuarioId ID do usuário logado
     * @return Lista de configurações criadas
     */
    @CacheEvict(value = "configuracoes-cards", key = "'ativos-' + #usuarioId")
    @Transactional
    public List<ConfiguracaoCardResponse> inicializarConfiguracoesPrimeiroAcesso(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Set<TipoCardDashboard> tipos = resolverTiposPermitidos(usuario.getTipoUsuarioNovo());

        int ordem = 1;
        for (TipoCardDashboard tipo : tipos) {
            if (!cardRepository.existsByUsuarioIdAndTipoCard(usuarioId, tipo)) {
                cardRepository.save(ConfiguracaoCardDashboard.builder()
                        .organizacao(usuario.getOrganizacao())
                        .usuario(usuario)
                        .tipoCard(tipo)
                        .ativo(true)
                        .ordemExibicao(ordem++)
                        .build());
            } else {
                ordem++;
            }
        }
        return listarConfiguracoes(usuarioId);
    }

    /**
     * Chamado ao criar um novo usuário — inicializa configs de cards já ativas.
     *
     * @param usuario Entidade do usuário recém-criado
     */
    @Transactional
    public void inicializarParaNovoUsuario(Usuario usuario) {
        Set<TipoCardDashboard> tipos = resolverTiposPermitidos(usuario.getTipoUsuarioNovo());
        int ordem = 1;
        for (TipoCardDashboard tipo : tipos) {
            cardRepository.save(ConfiguracaoCardDashboard.builder()
                    .organizacao(usuario.getOrganizacao())
                    .usuario(usuario)
                    .tipoCard(tipo)
                    .ativo(true)
                    .ordemExibicao(ordem++)
                    .build());
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Resolve quais cards são permitidos para cada tipo de usuário.
     *
     * @param tipo TipoUsuarioNovo do usuário
     * @return Conjunto de TipoCardDashboard permitidos
     */
    public Set<TipoCardDashboard> resolverTiposPermitidos(TipoUsuarioNovo tipo) {
        if (tipo == null) {
            return CARDS_ADMIN_ORG;
        }
        return switch (tipo) {
            case PROFISSIONAL  -> CARDS_PROFISSIONAL;
            case SUPER_ADMIN   -> CARDS_SUPER_ADMIN;
            case RECEPCIONISTA -> CARDS_RECEPCIONISTA;
            default            -> CARDS_ADMIN_ORG;
        };
    }

    private ConfiguracaoCardResponse toResponse(ConfiguracaoCardDashboard config) {
        return new ConfiguracaoCardResponse(
                config.getId(),
                config.getTipoCard(),
                config.getTipoCard().getDescricao(),
                config.getAtivo(),
                config.getOrdemExibicao()
        );
    }
}
