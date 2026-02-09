package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.usuario.UsuarioInputPort;
import br.com.saudeConecta.application.port.out.usuario.UsuarioOutputPort;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.usuario.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UsuarioService implements UsuarioInputPort {
    private final UsuarioRepository usuarioRepository;

    private final UsuarioOutputPort usuarioOutputPort;
    private final PasswordEncoder passwordEncoder;
    private final PacienteService pacienteService;
    private final ProfissionalRepository profissionalRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final PacienteRepository pacienteRepository;
    private final SecretariaRepository secretariaRepository;

    public UsuarioService(
            UsuarioOutputPort usuarioOutputPort,
            PasswordEncoder passwordEncoder,
            @Lazy PacienteService pacienteService,
            ProfissionalRepository profissionalRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            PacienteRepository pacienteRepository,
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository) {
        this.usuarioOutputPort = usuarioOutputPort;
        this.passwordEncoder = passwordEncoder;
        this.pacienteService = pacienteService;
        this.profissionalRepository = profissionalRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.pacienteRepository = pacienteRepository;
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
    }




    @Transactional
    public void bloquearUsuariobyOrg(BloquearUsuarioRequest request) {
        log.info("Alterando status do usuário ID: {} para {}",
                request.codigoUsuario(), request.status() == 0 ? "BLOQUEADO" : "ATIVO");

        // Obtém o ID da organização do contexto do tenant
        Long organizacaoId = TenantContext.getCurrentTenant();;

        // Busca o usuário com relacionamentos filtrado por organização
        Usuario usuario = usuarioRepository.findById(request.codigoUsuario())
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para alteração de status: {}", request.codigoUsuario());
                    return new IllegalArgumentException("Usuário não encontrado");
                });

        // Atualiza status do usuário
        StatusUsuario statusEnum = request.status() == 0 ? StatusUsuario.INATIVO : StatusUsuario.ATIVO;
        usuario.setStatus(statusEnum);
        usuarioRepository.save(usuario);

        // Atualiza status nas tabelas relacionadas
        atualizarStatusRelacionamentos(request.codigo(), organizacaoId, request.status());

        log.info("Status do usuário ID: {} alterado com sucesso", request.codigoUsuario());
    }

    private void atualizarStatusRelacionamentos(Long codigo, Long organizacaoId, int status) {
        // Atualiza Profissional
        profissionalRepository.findByUsuarioAndOrganizacao_Id(codigo, organizacaoId)
                .ifPresent(profissional -> {
                    profissional.setStatus(status == 0 ?
                            StatusProfissional.INATIVO : StatusProfissional.ATIVO);
                    profissionalRepository.save(profissional);
                    log.debug("Status do profissional ID: {} atualizado", profissional.getId());
                });

        // Atualiza Secretaria
        secretariaRepository.findByOrganizacao_IdAndUsuario_Id(codigo, organizacaoId)
                .ifPresent(secretaria -> {
                    secretaria.setStatus(status == 0 ?
                            StatusSecretaria.INATIVO : StatusSecretaria.ATIVO);
                    secretariaRepository.save(secretaria);
                    log.debug("Status da secretária ID: {} atualizado", secretaria.getId());
                });

        // Atualiza AdminOrganizacao
        adminOrganizacaoRepository.findByUsuarioAndOrganizacao_Id(codigo, organizacaoId)
                .ifPresent(admin -> {
                    admin.setStatus(status == 0 ?
                            AdminOrganizacao.StatusAdmin.INATIVO : AdminOrganizacao.StatusAdmin.ATIVO);
                    adminOrganizacaoRepository.save(admin);
                    log.debug("Status do admin ID: {} atualizado", admin.getId());
                });
    }




























    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        log.debug("Buscando usuário por ID: {}", id);
        return usuarioOutputPort.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        log.debug("Buscando usuário por login: {}", login);
        return usuarioOutputPort.findUsuarioByLogin(login);
    }

    @Override
    public UserDetails buscarUserDetailsPorLogin(String login) {
        log.debug("Buscando UserDetails por login: {}", login);
        return usuarioOutputPort.findByLogin(login);
    }

    @Override
    public List<Usuario> buscarTodos() {
        log.debug("Buscando todos os usuários");
        return usuarioOutputPort.findAll();
    }

    @Override
    public Page<Usuario> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os usuários com paginação");
        return usuarioOutputPort.findAll(pageable);
    }

    @Override
    public boolean existePorLogin(String login) {
        log.debug("Verificando existência de usuário por login: {}", login);
        return usuarioOutputPort.existsByLogin(login);
    }

    @Override
    public Usuario cadastrar(Usuario usuario) {
        log.info("Cadastrando novo usuário: {}", usuario.getLogin());
        Usuario usuarioSalvo = usuarioOutputPort.save(usuario);
        log.info("Usuário cadastrado com sucesso. ID: {}", usuarioSalvo.getId());
        return usuarioSalvo;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão do usuário ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!usuarioOutputPort.existsById(id)) {
            log.warn("Usuário não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            usuarioOutputPort.deleteById(id);
            log.info("Usuário ID: {} excluído com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir usuário ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }

    /**
     * Cadastra usuário com validação de login existente e criptografia de senha.
     * @param dados DTO com dados do usuário
     * @return Usuario cadastrado
     * @throws IllegalStateException se login já existir
     */
    public Usuario cadastrarComValidacao(CadastrarUsuarioRequest dados) {
        log.info("Cadastrando usuário com validação: {}", dados.login());
        
        if (existePorLogin(dados.login())) {
            log.warn("Login já existe: {}", dados.login());
            throw new IllegalStateException("Login já existe");
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());
        Usuario usuario = new Usuario(dados, senhaCriptografada);
        
        return cadastrar(usuario);
    }

    /**
     * Bloqueia ou desbloqueia usuário.
     * @param id ID do usuário
     * @param status 0 para bloqueado, 1 para ativo
     * @throws IllegalArgumentException se usuário não for encontrado
     */

    /**
     * Busca todos os pacientes.
     * @return Lista de PacienteResponse
     */
    public List<PacienteResponse> buscarTodosPacientes() {
        log.debug("Buscando todos os pacientes");
        return pacienteService.buscarTodos().stream()
            .map(PacienteResponse::new)
            .toList();
    }

    /**
     * Troca a senha do usuário.
     * @param id ID do usuário
     * @param novaSenha Nova senha (será criptografada)
     * @throws IllegalArgumentException se usuário não for encontrado
     */
    public void trocarSenha(Long id, String novaSenha) {
        log.info("Trocando senha do usuário ID: {}", id);
        
        var usuarioOpt = buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            log.warn("Usuário não encontrado para troca de senha: {}", id);
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        
        var usuario = usuarioOpt.get();
        String senhaCriptografada = passwordEncoder.encode(novaSenha);
        usuario.setSenha(senhaCriptografada);
        usuarioOutputPort.save(usuario);
        log.info("Senha do usuário ID: {} alterada com sucesso", id);
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
        Profissional profissional = profissionalRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
        AdminOrganizacao admin = adminOrganizacaoRepository.findByUsuarioIdWithRelations(usuarioId).orElse(null);
        
        return Optional.of(UsuarioPerfilCompletoResponse.fromEntities(usuario, profissional, admin));
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
}
