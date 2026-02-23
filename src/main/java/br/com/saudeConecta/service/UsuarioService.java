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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static br.com.saudeConecta.domain.usuario.TipoUsuarioNovo.ADMIN_ORG;

@Service
@Slf4j
public class UsuarioService   {
    private final UsuarioRepository usuarioRepository;

     private final PasswordEncoder passwordEncoder;
     private final ProfissionalRepository profissionalRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final SecretariaRepository secretariaRepository;

    public UsuarioService(
             PasswordEncoder passwordEncoder,
             ProfissionalRepository profissionalRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            PacienteRepository pacienteRepository,
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository) {
         this.passwordEncoder = passwordEncoder;
         this.profissionalRepository = profissionalRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
    }




    @Transactional
    public void bloquearUsuariobyOrg(BloquearUsuarioRequest request) {
        Long organizacaoId = TenantContext.getCurrentTenant();

        log.info("Bloqueando usuário ID: {} (registro ID: {}) para {} na organização {}",
                request.codigoUsuario(),
                request.codigo(),
                request.status() == 0 ? "INATIVO" : "ATIVO",
                organizacaoId);

        // 1. Busca e atualiza o Usuario principal
        Usuario usuario = usuarioRepository.findById(request.codigoUsuario())
                .filter(u -> organizacaoId.equals(u.getOrganizacaoId()))
                .orElseThrow(() -> {
                    log.warn("Usuário {} não encontrado ou não pertence à organização {}",
                            request.codigoUsuario(), organizacaoId);
                    return new IllegalArgumentException("Usuário não encontrado ou sem permissão");
                });

        // Validação: não pode bloquear super admin
        if (usuario.isSuperAdmin()) {
            throw new IllegalArgumentException("Não é possível bloquear um Super Admin");
        }

        // Atualiza status do usuário
        StatusUsuario novoStatus = request.status() == 0
                ? StatusUsuario.INATIVO
                : StatusUsuario.ATIVO;
        usuario.setStatus(novoStatus);
        usuarioRepository.save(usuario);

        // 2. Atualiza o registro específico baseado no tipo de usuário
        atualizarStatusPerfil(request.codigo(), organizacaoId, usuario.getTipoUsuarioNovo(), novoStatus);

        log.info("Usuário ID: {} e seu perfil bloqueados com sucesso", request.codigoUsuario());
    }

    private void atualizarStatusPerfil(Long codigoPerfil, Long organizacaoId,
                                       TipoUsuarioNovo tipoUsuario, StatusUsuario status) {

        switch (tipoUsuario) {
            case ADMIN_ORG -> {
                adminOrganizacaoRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                        .ifPresentOrElse(
                                admin -> {
                                    admin.setStatus(status == StatusUsuario.ATIVO
                                            ? AdminOrganizacao.StatusAdmin.ATIVO
                                            : AdminOrganizacao.StatusAdmin.INATIVO);
                                    adminOrganizacaoRepository.save(admin);
                                    log.debug("Status atualizado em AdminOrganizacao ID: {}", codigoPerfil);
                                },
                                () -> log.warn("AdminOrganizacao ID {} não encontrado na org {}",
                                        codigoPerfil, organizacaoId)
                        );
            }

            case RECEPCIONISTA -> {
                secretariaRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                        .ifPresentOrElse(
                                secretaria -> {
                                    secretaria.setStatus(status == StatusUsuario.ATIVO
                                            ? StatusSecretaria.ATIVO
                                            : StatusSecretaria.INATIVO);
                                    secretariaRepository.save(secretaria);
                                    log.debug("Status atualizado em Secretaria ID: {}", codigoPerfil);
                                },
                                () -> log.warn("Secretaria ID {} não encontrada na org {}",
                                        codigoPerfil, organizacaoId)
                        );
            }

            case PROFISSIONAL -> {
                profissionalRepository.findByIdAndOrganizacao_Id(codigoPerfil, organizacaoId)
                        .ifPresentOrElse(
                                profissional -> {
                                    profissional.setStatus(status == StatusUsuario.ATIVO
                                            ? StatusProfissional.ATIVO
                                            : StatusProfissional.INATIVO);
                                    profissionalRepository.save(profissional);
                                    log.debug("Status atualizado em Profissional ID: {}", codigoPerfil);
                                },
                                () -> log.warn("Profissional ID {} não encontrado na org {}",
                                        codigoPerfil, organizacaoId)
                        );
            }

            default -> log.warn("Tipo de usuário {} não possui perfil específico para bloquear",
                    tipoUsuario);
        }
    }

    @Transactional(readOnly = true)
    public TodosUsuariosAgrupadosResponse buscarTodosAdminOrgsSuperAdmin() {
        log.debug("SUPER_ADMIN: buscando todos os AdminOrgs do sistema");
        var administradores = adminOrganizacaoRepository.findAllWithRelations().stream()
                .map(TodosUsuariosAgrupadosResponse.AdminResumo::fromEntity)
                .toList();
        return new TodosUsuariosAgrupadosResponse(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), administradores);
    }

    @Transactional(readOnly = true)
    public TodosUsuariosAgrupadosResponse buscarTodosAgrupados(Long organizacaoId) {
        log.debug("Buscando todos os usuários agrupados para organização ID: {}", organizacaoId);

        var pacientes = pacienteRepository.findByOrganizacao_Id(organizacaoId).stream()
                .map(TodosUsuariosAgrupadosResponse.PacienteResumo::fromEntity)
                .toList();

        var profissionais = profissionalRepository.findByOrganizacao_IdWithUsuario(organizacaoId);

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
            log.warn("Usuário não encontrado para troca de senha: {}", id);
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        var usuario = usuarioOpt.get();
        String senhaCriptografada = passwordEncoder.encode(novaSenha);
        usuario.setSenha(senhaCriptografada);
        usuarioRepository.save(usuario);
        log.info("Senha do usuário ID: {} alterada com sucesso", id);
    }


    public Optional<Usuario> buscarPorId(Long id) {
        log.debug("Buscando usuário por ID: {}", id);
        return usuarioRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public Optional<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(Long usuarioId) {
        log.debug("Buscando perfil completo do usuário ID: {}", usuarioId);

        Optional<Usuario> usuarioOpt = buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty()) {
            log.warn("Usuário não encontrado: {}", usuarioId);
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();
        TipoUsuarioNovo tipoUsuario = usuario.getTipoUsuarioNovo();
        
        if (tipoUsuario == null) {
            log.warn("Usuário ID: {} não possui tipo_usuario_novo definido", usuarioId);
            return Optional.empty();
        }

        log.debug("Tipo de usuário identificado: {}", tipoUsuario);

        Profissional profissional = null;
        AdminOrganizacao admin = null;
        Secretaria secretaria = null;

        switch (tipoUsuario) {
            case PROFISSIONAL -> {
                profissional = profissionalRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
                if (profissional != null) {
                    log.debug("Profissional encontrado ID: {} com endereço: {}", 
                        profissional.getId(), 
                        profissional.getEndereco() != null ? "Sim" : "Não");
                }
            }
            
            case ADMIN_ORG -> {
                admin = adminOrganizacaoRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
                if (admin != null) {
                    log.debug("AdminOrganizacao encontrado ID: {}", admin.getId());
                }
            }
            
            case RECEPCIONISTA -> {
                secretaria = secretariaRepository.findByUsuario_Id(usuarioId).orElse(null);
                if (secretaria != null) {
                    log.debug("Secretaria encontrada ID: {}", secretaria.getId());
                } else {
                    log.warn("Secretaria não encontrada para usuário ID: {}", usuarioId);
                }
            }
            
            default -> log.warn("Tipo de usuário {} não possui perfil específico implementado", tipoUsuario);
        }

        return Optional.of(UsuarioPerfilCompletoResponse.fromEntities(usuario, profissional, admin, secretaria));
    }


}
