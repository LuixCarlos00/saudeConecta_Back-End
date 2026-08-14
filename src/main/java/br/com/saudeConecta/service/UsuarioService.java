package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.usuario.*;
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

    public UsuarioService(
             PasswordEncoder passwordEncoder,
             ProfissionalRepository profissionalRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            PacienteRepository pacienteRepository,
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository,
            CacheEvictionService cacheEvictionService) {
         this.passwordEncoder = passwordEncoder;
         this.profissionalRepository = profissionalRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cacheEvictionService = cacheEvictionService;
    }




    @Transactional
    public void bloquearUsuariobyOrg(BloquearUsuarioRequest request) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        boolean isSuperAdmin = (organizacaoId == null);

        log.info("Bloqueando usuario ID: {} (registro ID: {}) para {} | orgId: {} | SUPER_ADMIN: {}",
                request.codigoUsuario(),
                request.codigo(),
                request.status() == 0 ? "INATIVO" : "ATIVO",
                organizacaoId,
                isSuperAdmin);

        // 1. Busca o Usuario principal
        Usuario usuario;
        if (isSuperAdmin) {
            usuario = usuarioRepository.findById(request.codigoUsuario())
                    .orElseThrow(() -> {
                        log.warn("Usuario {} nao encontrado", request.codigoUsuario());
                        return new IllegalArgumentException("usuario nao encontrado");
                    });
        } else {
            usuario = usuarioRepository.findById(request.codigoUsuario())
                    .filter(u -> organizacaoId.equals(u.getOrganizacaoId()))
                    .orElseThrow(() -> {
                        log.warn("Usuario {} nao encontrado ou nao pertence a organizacao {}",
                                request.codigoUsuario(), organizacaoId);
                        return new IllegalArgumentException("Usuario nao encontrado ou sem permissao");
                    });
        }

        // Validação: nao pode bloquear root
        if (usuario.isRoot()) {
            throw new IllegalArgumentException("nao é possível bloquear um Root");
        }

        StatusUsuario novoStatus = request.status() == 0
                ? StatusUsuario.INATIVO
                : StatusUsuario.ATIVO;

        // 2. Se ROOT bloqueando GESTOR → bloqueio em cascata (toda a organizacao)
        if (isSuperAdmin && usuario.getTipoUsuarioNovo() == TipoUsuarioNovo.GESTOR) {
            bloquearTenantEmCascata(usuario, request.codigo(), novoStatus);
        } else {
            // Bloqueio individual
            usuario.setStatus(novoStatus);
            usuarioRepository.save(usuario);

            Long orgIdPerfil = isSuperAdmin ? usuario.getOrganizacaoId() : organizacaoId;
            atualizarStatusPerfil(request.codigo(), orgIdPerfil, usuario.getTipoUsuarioNovo(), novoStatus);
        }

        Long orgIdAfetada = isSuperAdmin ? usuario.getOrganizacaoId() : organizacaoId;
        cacheEvictionService.evictPerfilEUsuariosAgrupados(request.codigoUsuario(), orgIdAfetada);

        log.info("usuario ID: {} bloqueado com sucesso (cascata: {})", request.codigoUsuario(),
                isSuperAdmin && usuario.getTipoUsuarioNovo() == TipoUsuarioNovo.GESTOR);
    }

    /**
     * Bloqueio em cascata: bloqueia/desbloqueia o AdminOrg e TODOS os usuarios da organizacao.
     * Chamado pelo SUPER_ADMIN ao bloquear um tenant (AdminOrg).
     */
    private void bloquearTenantEmCascata(Usuario adminUsuario, Long adminPerfilId, StatusUsuario novoStatus) {
        Long orgId = adminUsuario.getOrganizacaoId();
        log.info("Bloqueio em cascata da organizacao ID: {} para status: {}", orgId, novoStatus);

        // 1. Bloqueia o AdminOrg principal
        adminUsuario.setStatus(novoStatus);
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

        // 2. Bloqueia/desbloqueia TODOS os outros usuarios da organizacao
        if (orgId != null) {
            List<Usuario> usuariosOrg = usuarioRepository.findByOrganizacao_Id(orgId);
            int count = 0;
            for (Usuario u : usuariosOrg) {
                if (!u.getId().equals(adminUsuario.getId()) && !u.isRoot()) {
                    u.setStatus(novoStatus);
                    usuarioRepository.save(u);
                    cacheEvictionService.evictPerfilUsuario(u.getId());
                    count++;
                }
            }
            log.info("Bloqueio em cascata: {} usuarios adicionais da organizacao {} atualizados para {}",
                    count, orgId, novoStatus);
        }
    }

    private void atualizarStatusPerfil(Long codigoPerfil, Long organizacaoId,
                                       TipoUsuarioNovo tipoUsuario, StatusUsuario status) {

        switch (tipoUsuario) {
            case GESTOR -> {
                adminOrganizacaoRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                        .ifPresentOrElse(
                                admin -> {
                                    admin.setStatus(status == StatusUsuario.ATIVO
                                            ? AdminOrganizacao.StatusAdmin.ATIVO
                                            : AdminOrganizacao.StatusAdmin.INATIVO);
                                    adminOrganizacaoRepository.save(admin);
                                    log.debug("Status atualizado em AdminOrganizacao ID: {}", codigoPerfil);
                                },
                                () -> log.warn("AdminOrganizacao ID {} nao encontrado na org {}",
                                        codigoPerfil, organizacaoId)
                        );
            }

            case ASSISTENTE -> {
                secretariaRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                        .ifPresentOrElse(
                                secretaria -> {
                                    secretaria.setStatus(status == StatusUsuario.ATIVO
                                            ? StatusSecretaria.ATIVO
                                            : StatusSecretaria.INATIVO);
                                    secretariaRepository.save(secretaria);
                                    log.debug("Status atualizado em Secretaria ID: {}", codigoPerfil);
                                },
                                () -> log.warn("Secretaria ID {} nao encontrada na org {}",
                                        codigoPerfil, organizacaoId)
                        );
            }

            case CLINICO -> {
                profissionalRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                        .ifPresentOrElse(
                                profissional -> {
                                    profissional.setStatus(status == StatusUsuario.ATIVO
                                            ? StatusProfissional.ATIVO
                                            : StatusProfissional.INATIVO);
                                    profissionalRepository.save(profissional);
                                    log.debug("Status atualizado em Profissional ID: {}", codigoPerfil);
                                },
                                () -> log.warn("Profissional ID {} nao encontrado na org {}",
                                        codigoPerfil, organizacaoId)
                        );
            }

            default -> log.warn("Tipo de usuario {} nao possui perfil especifico para bloquear",
                    tipoUsuario);
        }
    }

    @Cacheable(value = "usuarios-agrupados", key = "'super-admin'")
    @Transactional(readOnly = true)
    public TodosUsuariosAgrupadosResponse buscarTodosAdminOrgsSuperAdmin() {
        log.debug("SUPER_ADMIN: buscando todos os AdminOrgs do sistema");
        var administradores = adminOrganizacaoRepository.findAllWithRelations().stream()
                .map(TodosUsuariosAgrupadosResponse.AdminResumo::fromEntity)
                .toList();
        return new TodosUsuariosAgrupadosResponse(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), administradores);
    }

    @Cacheable(value = "usuarios-agrupados", key = "#organizacaoId")
    @Transactional(readOnly = true)
    public TodosUsuariosAgrupadosResponse buscarTodosAgrupados(Long organizacaoId) {
        log.debug("Buscando todos os usuarios agrupados para organizacao ID: {}", organizacaoId);

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





    public void trocarSenharUsuariobyOrg(Long id, String novaSenha) {

        var usuarioOpt = buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            log.warn("usuario nao encontrado para troca de senha: {}", id);
            throw new IllegalArgumentException("usuario nao encontrado");
        }

        var usuario = usuarioOpt.get();
        String senhaCriptografada = passwordEncoder.encode(novaSenha);
        usuario.setSenha(senhaCriptografada);
        usuarioRepository.save(usuario);
        cacheEvictionService.evictPerfilUsuario(id);
        log.info("Senha do usuario ID: {} alterada com sucesso", id);
    }


    public Optional<Usuario> buscarPorId(Long id) {
        log.debug("Buscando usuario por ID: {}", id);
        return usuarioRepository.findById(id);
    }


    @Cacheable(value = "perfil-usuario", key = "#usuarioId")
    @Transactional(readOnly = true)
    public Optional<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(Long usuarioId) {
        log.debug("Buscando perfil completo do usuario ID: {}", usuarioId);

        Optional<Usuario> usuarioOpt = usuarioRepository.findByIdWithOrganizacao(usuarioId);
        if (usuarioOpt.isEmpty()) {
            log.warn("Usuario nao encontrado: {}", usuarioId);
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();
        TipoUsuarioNovo tipoUsuario = usuario.getTipoUsuarioNovo();
        
        if (tipoUsuario == null) {
            log.warn("usuario ID: {} nao possui tipo_usuario_novo definido", usuarioId);
            return Optional.empty();
        }

        log.debug("Tipo de usuario identificado: {}", tipoUsuario);

        Profissional profissional = null;
        AdminOrganizacao admin = null;
        Secretaria secretaria = null;

        switch (tipoUsuario) {
            case CLINICO -> {
                profissional = profissionalRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
                if (profissional != null) {
                    log.debug("Profissional encontrado ID: {} com endereço: {}",
                        profissional.getId(),
                        profissional.getEndereco() != null ? "Sim" : "nao");
                }
            }
            
            case GESTOR -> {
                admin = adminOrganizacaoRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
                if (admin != null) {
                    log.debug("AdminOrganizacao encontrado ID: {}", admin.getId());
                }
            }
            
            case ASSISTENTE -> {
                secretaria = secretariaRepository.findByUsuario_Id(usuarioId).orElse(null);
                if (secretaria != null) {
                    log.debug("Secretaria encontrada ID: {}", secretaria.getId());
                } else {
                    log.warn("Secretaria nao encontrada para usuario ID: {}", usuarioId);
                }
            }
            
            default -> log.warn("Tipo de usuario {} nao possui perfil especifico implementado", tipoUsuario);
        }

        return Optional.of(UsuarioPerfilCompletoResponse.fromEntities(usuario, profissional, admin, secretaria));
    }


}
