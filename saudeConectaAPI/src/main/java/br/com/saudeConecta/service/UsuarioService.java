package br.com.saudeConecta.service;

 import br.com.saudeConecta.domain.admin.AdminOrganizacao;

import br.com.saudeConecta.domain.profissional.StatusProfissional;
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

















//
//
//
//
//    public Optional<Usuario> buscarPorLogin(String login) {
//        log.debug("Buscando usuário por login: {}", login);
//        return usuarioOutputPort.findUsuarioByLogin(login);
//    }
//
//
//    public UserDetails buscarUserDetailsPorLogin(String login) {
//        log.debug("Buscando UserDetails por login: {}", login);
//        return usuarioOutputPort.findByLogin(login);
//    }
//
//
//    public List<Usuario> buscarTodos() {
//        log.debug("Buscando todos os usuários");
//        return usuarioOutputPort.findAll();
//    }
//
//
//    public Page<Usuario> buscarTodos(Pageable pageable) {
//        log.debug("Buscando todos os usuários com paginação");
//        return usuarioOutputPort.findAll(pageable);
//    }
//
//
//    public boolean existePorLogin(String login) {
//        log.debug("Verificando existência de usuário por login: {}", login);
//        return usuarioOutputPort.existsByLogin(login);
//    }
//
//
//    public Usuario cadastrar(Usuario usuario) {
//        log.info("Cadastrando novo usuário: {}", usuario.getLogin());
//        Usuario usuarioSalvo = usuarioOutputPort.save(usuario);
//        log.info("Usuário cadastrado com sucesso. ID: {}", usuarioSalvo.getId());
//        return usuarioSalvo;
//    }
//
//
//    public void deletar(Long id) throws Exception {
//        log.info("Iniciando exclusão do usuário ID: {}", id);
//
//        if (id == null || id <= 0) {
//            log.warn("Tentativa de exclusão com ID inválido: {}", id);
//            throw new IllegalArgumentException("ID inválido");
//        }
//
//        if (!usuarioOutputPort.existsById(id)) {
//            log.warn("Usuário não encontrado para exclusão ID: {}", id);
//            throw new Exception("Registro não encontrado");
//        }
//
//        try {
//            usuarioOutputPort.deleteById(id);
//            log.info("Usuário ID: {} excluído com sucesso", id);
//        } catch (Exception e) {
//            log.error("Erro ao excluir usuário ID: {}", id, e);
//            throw new Exception("Violação de Integridade", e);
//        }
//    }
//
//    /**
//     * Cadastra usuário com validação de login existente e criptografia de senha.
//     * @param dados DTO com dados do usuário
//     * @return Usuario cadastrado
//     * @throws IllegalStateException se login já existir
//     */
//    public Usuario cadastrarComValidacao(CadastrarUsuarioRequest dados) {
//        log.info("Cadastrando usuário com validação: {}", dados.login());
//
//        if (existePorLogin(dados.login())) {
//            log.warn("Login já existe: {}", dados.login());
//            throw new IllegalStateException("Login já existe");
//        }
//
//        String senhaCriptografada = passwordEncoder.encode(dados.senha());
//        Usuario usuario = new Usuario(dados, senhaCriptografada);
//
//        return cadastrar(usuario);
//    }

//    /**
//     * Bloqueia ou desbloqueia usuário.
//     * @param id ID do usuário
//     * @param status 0 para bloqueado, 1 para ativo
//     * @throws IllegalArgumentException se usuário não for encontrado
//     */
//
//    /**
//     * Busca todos os pacientes.
//     * @return Lista de PacienteResponse
//     */
//    public List<PacienteResponse> buscarTodosPacientes() {
//        log.debug("Buscando todos os pacientes");
//        return pacienteService.buscarTodos().stream()
//            .map(PacienteResponse::new)
//            .toList();
//    }
//
//    /**
//     * Troca a senha do usuário.
//     * @param id ID do usuário
//     * @param novaSenha Nova senha (será criptografada)
//     * @throws IllegalArgumentException se usuário não for encontrado
//     */
//
//
//    @Transactional(readOnly = true)
//    public Optional<UsuarioPerfilCompletoResponse> buscarPerfilCompleto(Long usuarioId) {
//        log.debug("Buscando perfil completo do usuário ID: {}", usuarioId);
//
//        Optional<Usuario> usuarioOpt = buscarPorId(usuarioId);
//        if (usuarioOpt.isEmpty()) {
//            log.warn("Usuário não encontrado: {}", usuarioId);
//            return Optional.empty();
//        }
//
//        Usuario usuario = usuarioOpt.get();
//        Profissional profissional = profissionalRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
//        AdminOrganizacao admin = adminOrganizacaoRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
//
//        return Optional.of(UsuarioPerfilCompletoResponse.fromEntities(usuario, profissional, admin));
//    }


}
