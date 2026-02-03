package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.usuario.UsuarioInputPort;
import br.com.saudeConecta.application.port.out.usuario.UsuarioOutputPort;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.presentation.dto.usuario.CadastrarUsuarioRequest;
import br.com.saudeConecta.presentation.dto.usuario.TodosUsuariosAgrupadosResponse;
import br.com.saudeConecta.presentation.dto.usuario.PacienteResponse;
import br.com.saudeConecta.presentation.dto.usuario.MedicoComUsuarioResponse;
import br.com.saudeConecta.presentation.dto.usuario.AdministradorComUsuarioResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UsuarioService implements UsuarioInputPort {

    private final UsuarioOutputPort usuarioOutputPort;
    private final PasswordEncoder passwordEncoder;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;
    private final AdministradorService administradorService;

    public UsuarioService(
            UsuarioOutputPort usuarioOutputPort,
            PasswordEncoder passwordEncoder,
            @Lazy PacienteService pacienteService,
            @Lazy MedicoService medicoService,
            @Lazy AdministradorService administradorService) {
        this.usuarioOutputPort = usuarioOutputPort;
        this.passwordEncoder = passwordEncoder;
        this.pacienteService = pacienteService;
        this.medicoService = medicoService;
        this.administradorService = administradorService;
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
    public void bloquear(Long id, int status) {
        log.info("Alterando status do usuário ID: {} para {}", id, status == 0 ? "BLOQUEADO" : "ATIVO");
        
        var usuarioOpt = buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            log.warn("Usuário não encontrado para alteração de status: {}", id);
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        
        var usuario = usuarioOpt.get();
        usuario.setStatus(status == 0 ? (byte) 0 : (byte) 1);
        cadastrar(usuario);
        log.info("Status do usuário ID: {} alterado com sucesso", id);
    }

    /**
     * Busca todos os usuários agrupados por tipo.
     * @return TodosUsuariosAgrupadosResponse com listas de cada tipo
     */
    public TodosUsuariosAgrupadosResponse buscarTodosAgrupados() {
        log.debug("Buscando todos os usuários agrupados por tipo");
        
        var pacientes = pacienteService.buscarTodos().stream()
            .map(PacienteResponse::new)
            .toList();
        var medicos = medicoService.buscarTodosComUsuario().stream()
            .map(MedicoComUsuarioResponse::new)
            .toList();
        var administradores = administradorService.buscarTodosComUsuario().stream()
            .map(AdministradorComUsuarioResponse::new)
            .toList();
        var secretarias = new ArrayList<>(); // TODO: implementar quando SecretariaService estiver disponível
        
        return new TodosUsuariosAgrupadosResponse(pacientes, medicos, secretarias, administradores);
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
}
