package br.com.saudeConecta.application.usuario;

import br.com.saudeConecta.application.assinatura.LimitePlanoService;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.usuario.dto.*;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.MotivoBloqueio;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UsuarioService   {
    private final UsuarioRepository usuarioRepository;

     private final PasswordEncoder passwordEncoder;
     private final ProfissionalRepository profissionalRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final SecretariaRepository secretariaRepository;
    private final CacheEvictionService cacheEvictionService;
    private final TenantHelper tenantHelper;
    private final LimitePlanoService limitePlanoService;

    public UsuarioService(
             PasswordEncoder passwordEncoder,
             ProfissionalRepository profissionalRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            PacienteRepository pacienteRepository,
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository,
            CacheEvictionService cacheEvictionService,
            TenantHelper tenantHelper,
            LimitePlanoService limitePlanoService) {
         this.passwordEncoder = passwordEncoder;
         this.profissionalRepository = profissionalRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.tenantHelper = tenantHelper;
        this.limitePlanoService = limitePlanoService;
    }

    @Transactional
    public void bloquearUsuariobyOrg(BloquearUsuarioRequest request) {
        // Falha fechada: sem tenant só passa quem tem ROLE_SUPER_ADMIN; os demais recebem 403.
        Long organizacaoId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        boolean isSuperAdmin = (organizacaoId == null);

        log.info("usuario.bloquear inicio usuarioId={} perfilId={} status={} superAdmin={}",
                request.codigoUsuario(),
                request.codigo(),
                request.status() == 0 ? "INATIVO" : "ATIVO",
                isSuperAdmin);

        // 1. Busca o Usuario principal
        Usuario usuario = buscarUsuarioParaBloqueio(request.codigoUsuario(), organizacaoId, isSuperAdmin);

        // Validação: nao pode bloquear root
        if (usuario.isRoot()) {
            throw new IllegalArgumentException("nao é possível bloquear um Root");
        }

        StatusUsuario novoStatus = request.status() == 0
                ? StatusUsuario.INATIVO
                : StatusUsuario.ATIVO;

        // 2. Se ROOT bloqueando GESTOR → bloqueio em cascata (toda a organizacao)
        boolean cascata = isSuperAdmin && usuario.getTipoUsuarioNovo() == TipoUsuarioNovo.GESTOR;
        if (cascata) {
            bloquearTenantEmCascata(usuario, request.codigo(), novoStatus);
        } else {
            bloquearIndividualmente(usuario, request.codigo(), novoStatus, isSuperAdmin, organizacaoId);
        }

        Long orgIdAfetada = isSuperAdmin ? usuario.getOrganizacaoId() : organizacaoId;
        cacheEvictionService.evictPerfilEUsuariosAgrupados(request.codigoUsuario(), orgIdAfetada);

        log.info("usuario.bloquear ok usuarioId={} status={} cascata={}", request.codigoUsuario(), novoStatus, cascata);
    }

    /**
     * Busca o usuário a bloquear: o SUPER_ADMIN alcança qualquer um; os demais só os da própria organização.
     *
     * @param usuarioId      usuário a bloquear
     * @param organizacaoId  organização do chamador (nula para SUPER_ADMIN)
     * @param isSuperAdmin   se o chamador é SUPER_ADMIN
     * @return usuário encontrado
     * @throws IllegalArgumentException se não existir ou for de outra clínica
     */
    private Usuario buscarUsuarioParaBloqueio(Long usuarioId, Long organizacaoId, boolean isSuperAdmin) {
        if (isSuperAdmin) {
            return usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> {
                        log.warn("usuario.bloquear negado motivo=NAO_ENCONTRADO usuarioId={}", usuarioId);
                        return new IllegalArgumentException("usuario nao encontrado");
                    });
        }
        return usuarioRepository.findById(usuarioId)
                .filter(u -> organizacaoId.equals(u.getOrganizacaoId()))
                .orElseThrow(() -> {
                    log.warn("usuario.bloquear negado motivo=NAO_ENCONTRADO_OU_OUTRA_CLINICA usuarioId={} organizacaoId={}",
                            usuarioId, organizacaoId);
                    return new IllegalArgumentException("Usuario nao encontrado ou sem permissao");
                });
    }

    private void bloquearIndividualmente(Usuario usuario, Long codigoPerfil, StatusUsuario novoStatus,
                                         boolean isSuperAdmin, Long organizacaoId) {
        Long orgIdPerfil = isSuperAdmin ? usuario.getOrganizacaoId() : organizacaoId;

        // Reativar ocupa uma vaga do plano, igual a um cadastro novo
        if (novoStatus == StatusUsuario.ATIVO && !usuario.isEnabled()) {
            validarLimiteParaReativar(usuario.getTipoUsuarioNovo(), orgIdPerfil);
        }

        usuario.setStatus(novoStatus);
        usuario.setMotivoBloqueio(novoStatus == StatusUsuario.INATIVO ? MotivoBloqueio.INDIVIDUAL : null);
        usuarioRepository.save(usuario);

        atualizarStatusPerfil(codigoPerfil, orgIdPerfil, usuario.getTipoUsuarioNovo(), novoStatus);
    }

    /**
     * Bloqueio em cascata: bloqueia/desbloqueia o AdminOrg e TODOS os usuarios da organizacao.
     * Chamado pelo SUPER_ADMIN ao bloquear um tenant (AdminOrg).
     */
    private void bloquearTenantEmCascata(Usuario adminUsuario, Long adminPerfilId, StatusUsuario novoStatus) {
        Long orgId = adminUsuario.getOrganizacaoId();
        log.info("usuario.bloquear cascata inicio organizacaoId={} status={}", orgId, novoStatus);

        boolean bloqueando = novoStatus == StatusUsuario.INATIVO;

        // 1. Bloqueia o AdminOrg principal
        adminUsuario.setStatus(novoStatus);
        adminUsuario.setMotivoBloqueio(bloqueando ? MotivoBloqueio.CASCATA : null);
        usuarioRepository.save(adminUsuario);

        // Atualiza perfil do AdminOrg
        adminOrganizacaoRepository.findById(adminPerfilId)
                .ifPresent(admin -> {
                    admin.setStatus(novoStatus == StatusUsuario.ATIVO
                            ? AdminOrganizacao.StatusAdmin.ATIVO
                            : AdminOrganizacao.StatusAdmin.INATIVO);
                    adminOrganizacaoRepository.save(admin);
                });

        cacheEvictionService.evictPerfilUsuario(adminUsuario.getId());

        // 2. Bloqueia/desbloqueia os outros usuarios da organizacao. Quem ja estava bloqueado
        //    individualmente fica de fora nos dois sentidos: o bloqueio nao sobrescreve o motivo
        //    e o desbloqueio so reativa quem a propria cascata bloqueou. Bloqueios anteriores a
        //    coluna motivo_bloqueio (null) nao sao reativados: o gestor reativa um a um.
        if (orgId != null) {
            List<Usuario> usuariosOrg = usuarioRepository.findByOrganizacao_Id(orgId);
            int count = 0;
            for (Usuario u : usuariosOrg) {
                if (u.getId().equals(adminUsuario.getId()) || u.isRoot()) {
                    continue;
                }
                if (afetadoPelaCascata(u, bloqueando)) {
                    u.setStatus(novoStatus);
                    u.setMotivoBloqueio(bloqueando ? MotivoBloqueio.CASCATA : null);
                    usuarioRepository.save(u);
                    cacheEvictionService.evictPerfilUsuario(u.getId());
                    count++;
                }
            }
            log.info("usuario.bloquear cascata ok organizacaoId={} usuariosAdicionais={} status={}",
                    orgId, count, novoStatus);
        }
    }

    /** Bloqueio atinge quem está ativo; desbloqueio só reativa quem a própria cascata bloqueou. */
    private boolean afetadoPelaCascata(Usuario u, boolean bloqueando) {
        if (bloqueando) {
            return u.isEnabled();
        }
        return !u.isEnabled() && u.getMotivoBloqueio() == MotivoBloqueio.CASCATA;
    }

    /**
     * Reativar um usuário consome uma vaga do plano; sem esta checagem o gestor contornava o
     * limite bloqueando e desbloqueando usuários.
     *
     * @param tipoUsuario   tipo do usuário reativado
     * @param organizacaoId organização dona da vaga
     * @throws br.com.saudeConecta.application.exception.LimitePlanoException se o limite estiver cheio
     */
    private void validarLimiteParaReativar(TipoUsuarioNovo tipoUsuario, Long organizacaoId) {
        if (tipoUsuario == null || organizacaoId == null) {
            return;
        }
        switch (tipoUsuario) {
            case GESTOR -> limitePlanoService.validarLimiteAdminOrg(organizacaoId);
            case CLINICO -> limitePlanoService.validarLimiteProfissional(organizacaoId);
            case ASSISTENTE -> limitePlanoService.validarLimiteSecretaria(organizacaoId);
            default -> {
                // Demais perfis (ex.: ROOT) não têm limite de plano a validar
            }
        }
    }

    private void atualizarStatusPerfil(Long codigoPerfil, Long organizacaoId,
                                       TipoUsuarioNovo tipoUsuario, StatusUsuario status) {

        switch (tipoUsuario) {
            case GESTOR -> adminOrganizacaoRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                    .ifPresentOrElse(
                            admin -> {
                                admin.setStatus(status == StatusUsuario.ATIVO
                                        ? AdminOrganizacao.StatusAdmin.ATIVO
                                        : AdminOrganizacao.StatusAdmin.INATIVO);
                                adminOrganizacaoRepository.save(admin);
                                log.debug("usuario.bloquear perfil tipo=ADMIN perfilId={}", codigoPerfil);
                            },
                            () -> log.warn("usuario.bloquear negado motivo=PERFIL_NAO_ENCONTRADO tipo=ADMIN perfilId={} organizacaoId={}",
                                    codigoPerfil, organizacaoId)
                    );

            case ASSISTENTE -> secretariaRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                    .ifPresentOrElse(
                            secretaria -> {
                                secretaria.setStatus(status == StatusUsuario.ATIVO
                                        ? StatusSecretaria.ATIVO
                                        : StatusSecretaria.INATIVO);
                                secretariaRepository.save(secretaria);
                                log.debug("usuario.bloquear perfil tipo=SECRETARIA perfilId={}", codigoPerfil);
                            },
                            () -> log.warn("usuario.bloquear negado motivo=PERFIL_NAO_ENCONTRADO tipo=SECRETARIA perfilId={} organizacaoId={}",
                                    codigoPerfil, organizacaoId)
                    );

            case CLINICO -> profissionalRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                    .ifPresentOrElse(
                            profissional -> {
                                profissional.setStatus(status == StatusUsuario.ATIVO
                                        ? StatusProfissional.ATIVO
                                        : StatusProfissional.INATIVO);
                                profissionalRepository.save(profissional);
                                log.debug("usuario.bloquear perfil tipo=PROFISSIONAL perfilId={}", codigoPerfil);
                            },
                            () -> log.warn("usuario.bloquear negado motivo=PERFIL_NAO_ENCONTRADO tipo=PROFISSIONAL perfilId={} organizacaoId={}",
                                    codigoPerfil, organizacaoId)
                    );

            default -> log.warn("usuario.bloquear negado motivo=TIPO_SEM_PERFIL tipo={}", tipoUsuario);
        }
    }

    @Cacheable(value = "usuarios-agrupados", key = "'super-admin'")
    @Transactional(readOnly = true)
    public TodosUsuariosAgrupadosResponse buscarTodosAdminOrgsSuperAdmin() {
        log.debug("usuario.agrupados buscar escopo=SUPER_ADMIN");
        var administradores = adminOrganizacaoRepository.findAllWithRelations().stream()
                .map(TodosUsuariosAgrupadosResponse.AdminResumo::fromEntity)
                .toList();
        return new TodosUsuariosAgrupadosResponse(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), administradores);
    }

    @Cacheable(value = "usuarios-agrupados", key = "#organizacaoId")
    @Transactional(readOnly = true)
    public TodosUsuariosAgrupadosResponse buscarTodosAgrupados(Long organizacaoId) {
        log.debug("usuario.agrupados buscar organizacaoId={}", organizacaoId);

        var pacientes = pacienteRepository.findByOrganizacao_Id(organizacaoId).stream()
                .map(TodosUsuariosAgrupadosResponse.PacienteResumo::fromEntity)
                .toList();

        var profissionais = profissionalRepository.findByOrganizacao_IdParaAgrupamento(organizacaoId);

        var medicos = profissionais.stream()
                .filter(p -> p.getTipoProfissional() != null &&
                        ("MEDICO".equalsIgnoreCase(p.getTipoProfissional().getCodigo()) ||
                                "DENTISTA".equalsIgnoreCase(p.getTipoProfissional().getCodigo())))
                .map(TodosUsuariosAgrupadosResponse.ProfissionalResumo::fromEntity)
                .toList();

        var secretarias = secretariaRepository.findByOrganizacao_IdWithUsuario(organizacaoId).stream()
                .map(TodosUsuariosAgrupadosResponse.SecretariaResumo::fromEntity)
                .toList();

        var administradores = adminOrganizacaoRepository.findByOrganizacao_IdWithUsuario(organizacaoId).stream()
                .map(TodosUsuariosAgrupadosResponse.AdminResumo::fromEntity)
                .toList();

        return new TodosUsuariosAgrupadosResponse(pacientes, medicos, secretarias, administradores);
    }

    /**
     * Troca a senha de um usuário, aplicando a regra de autorização do produto:
     * qualquer usuário troca a própria senha; o gestor (admin da organização)
     * troca a senha dos usuários da mesma organização, exceto a do ROOT.
     *
     * @param id        ID do usuário que terá a senha alterada
     * @param novaSenha nova senha em texto puro (é criptografada antes de salvar)
     * @throws IllegalArgumentException    se o usuário alvo não existir
     * @throws TenantAccessDeniedException se o usuário logado não puder trocar a senha do alvo
     */
    @Transactional
    public void trocarSenharUsuariobyOrg(Long id, String novaSenha) {

        var usuarioOpt = buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            log.warn("usuario.senha negado motivo=NAO_ENCONTRADO usuarioId={}", id);
            throw new IllegalArgumentException("usuario nao encontrado");
        }

        var usuario = usuarioOpt.get();
        validarPermissaoTrocaSenha(usuario);
        String senhaCriptografada = passwordEncoder.encode(novaSenha);
        usuario.setSenha(senhaCriptografada);
        usuarioRepository.save(usuario);
        cacheEvictionService.evictPerfilUsuario(id);
        log.info("usuario.senha ok usuarioId={}", id);
    }

    /**
     * Garante que o usuário logado (vindo do JWT, via TenantContext) pode trocar a senha do alvo.
     * A organização do logado vem do token, não do banco, pelo mesmo motivo do resto do sistema:
     * o token é a fonte do tenant da requisição. O ROOT só troca a própria senha por aqui;
     * para agir em nome de uma clínica ele usa a senha master (login como o usuário escolhido).
     *
     * @param alvo usuário que terá a senha alterada
     * @throws TenantAccessDeniedException quando a troca não é permitida
     */
    private void validarPermissaoTrocaSenha(Usuario alvo) {
        Long usuarioLogadoId = TenantContext.getCurrentUser();
        if (usuarioLogadoId == null) {
            throw negarTrocaSenha(alvo.getId(), "SEM_USUARIO_LOGADO");
        }

        if (usuarioLogadoId.equals(alvo.getId())) {
            return;
        }

        if (alvo.getTipoUsuarioNovo() != null && alvo.getTipoUsuarioNovo().isRoot()) {
            throw negarTrocaSenha(alvo.getId(), "ALVO_ROOT");
        }

        var usuarioLogado = usuarioRepository.findById(usuarioLogadoId)
                .orElseThrow(() -> negarTrocaSenha(alvo.getId(), "LOGADO_INEXISTENTE"));

        boolean isGestor = usuarioLogado.getTipoUsuarioNovo() != null
                && usuarioLogado.getTipoUsuarioNovo().isGestor();
        Long organizacaoLogado = TenantContext.getCurrentTenant();
        Long organizacaoAlvo = alvo.getOrganizacao() != null ? alvo.getOrganizacao().getId() : null;

        if (!isGestor || organizacaoLogado == null || !organizacaoLogado.equals(organizacaoAlvo)) {
            throw negarTrocaSenha(alvo.getId(), "SEM_PERMISSAO");
        }
    }

    private TenantAccessDeniedException negarTrocaSenha(Long alvoId, String motivo) {
        log.warn("usuario.senha negado motivo={} alvoId={}", motivo, alvoId);
        return new TenantAccessDeniedException("Acesso negado: sem permissao para trocar a senha deste usuario");
    }

    public Optional<Usuario> buscarPorId(Long id) {
        log.debug("usuario.buscar usuarioId={}", id);
        return usuarioRepository.findById(id);
    }

    @Cacheable(value = "perfil-usuario", key = "#usuarioId")
    @Transactional(readOnly = true)
    public Optional<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(Long usuarioId) {
        log.debug("usuario.perfil buscar usuarioId={}", usuarioId);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByIdWithOrganizacao(usuarioId);
        if (usuarioOpt.isEmpty()) {
            log.warn("usuario.perfil negado motivo=NAO_ENCONTRADO usuarioId={}", usuarioId);
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();
        TipoUsuarioNovo tipoUsuario = usuario.getTipoUsuarioNovo();
        
        if (tipoUsuario == null) {
            log.warn("usuario.perfil negado motivo=SEM_TIPO_USUARIO usuarioId={}", usuarioId);
            return Optional.empty();
        }

        log.debug("usuario.perfil tipo={}", tipoUsuario);

        Profissional profissional = null;
        AdminOrganizacao admin = null;
        Secretaria secretaria = null;

        switch (tipoUsuario) {
            case CLINICO -> {
                profissional = profissionalRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
                if (profissional != null) {
                    log.debug("usuario.perfil profissional profissionalId={} comEndereco={}",
                            profissional.getId(),
                            profissional.getEndereco() != null);
                }
            }
            
            case GESTOR -> {
                admin = adminOrganizacaoRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
                if (admin != null) {
                    log.debug("usuario.perfil admin adminId={}", admin.getId());
                }
            }
            
            case ASSISTENTE -> {
                secretaria = secretariaRepository.findByUsuario_Id(usuarioId).orElse(null);
                if (secretaria != null) {
                    log.debug("usuario.perfil secretaria secretariaId={}", secretaria.getId());
                } else {
                    log.warn("usuario.perfil negado motivo=SECRETARIA_NAO_ENCONTRADA usuarioId={}", usuarioId);
                }
            }
            
            default -> log.warn("usuario.perfil negado motivo=TIPO_SEM_PERFIL_IMPLEMENTADO tipo={}", tipoUsuario);
        }

        return Optional.of(UsuarioPerfilCompletoResponse.fromEntities(usuario, profissional, admin, secretaria));
    }

}
