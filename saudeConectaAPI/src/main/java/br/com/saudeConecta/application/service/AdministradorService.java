package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.administrador.AdministradorInputPort;
import br.com.saudeConecta.application.port.out.administrador.AdministradorOutputPort;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.CredenciaisEmailService;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.administrador.CadastrarAdministradorCompletoRequest;
import br.com.saudeConecta.presentation.dto.administrador.CadastrarAdministradorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministradorService implements AdministradorInputPort {

    private final AdministradorOutputPort administradorOutputPort;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredenciaisEmailService credenciaisEmailService;
    
    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    @Override
    public Optional<Administrador> buscarPorId(Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return administradorOutputPort.findById(id);
    }

    @Override
    public Optional<Administrador> buscarPorIdUsuario(Long usuarioId) {
        log.debug("Buscando administrador por ID de usuário: {}", usuarioId);
        return administradorOutputPort.findByAdmUsuario_Id(usuarioId);
    }

    @Override
    public List<Administrador> buscarTodos() {
        log.debug("Buscando todos os administradores");
        return administradorOutputPort.findAll();
    }

    public List<Administrador> buscarTodosComUsuario() {
        log.debug("Buscando todos os administradores com dados do usuário");
        return administradorOutputPort.findAllWithUsuario();
    }

    @Override
    public Page<Administrador> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os administradores com paginação");
        return administradorOutputPort.findAll(pageable);
    }

    @Override
    public Administrador cadastrar(Administrador administrador) {
        log.info("Cadastrando novo administrador: {}", administrador.getAdmNome());
        Administrador administradorSalvo = administradorOutputPort.save(administrador);
        log.info("Administrador cadastrado com sucesso. ID: {}", administradorSalvo.getAdmCodigo());
        return administradorSalvo;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão do administrador ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!administradorOutputPort.existsById(id)) {
            log.warn("Administrador não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            administradorOutputPort.deleteById(id);
            log.info("Administrador ID: {} excluído com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir administrador ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }

    /**
     * Cadastra administrador com validação de usuário.
     * @param dados DTO com dados do administrador
     * @return Administrador cadastrado
     * @throws IllegalArgumentException se usuário não for encontrado
     */
    public Administrador cadastrarComUsuario(CadastrarAdministradorRequest dados) {
        log.info("Cadastrando administrador com usuário: {}", dados.admNome());
        
        var usuarioOptional = usuarioRepository.findById(dados.admUsuario());
        if (usuarioOptional.isEmpty()) {
            log.warn("Usuário não encontrado para cadastro de administrador: {}", dados.admUsuario());
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        Usuario usuario = usuarioOptional.get();
        Administrador administrador = new Administrador(dados, usuario);
        
        return cadastrar(administrador);
    }

    /**
     * Cadastra administrador completo: cria usuário com CPF como login, gera senha com BCrypt e envia por email.
     */
    public Administrador cadastrarCompleto(CadastrarAdministradorCompletoRequest dados) {
        log.info("Cadastrando administrador completo: {} com CPF: {}", dados.admNome(), dados.admCpf());
        
        String cpfLimpo = limparCpf(dados.admCpf());
        
        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            log.warn("CPF já cadastrado como login: {}", cpfLimpo);
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }
        
        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);
        
        Usuario usuario = new Usuario();
        usuario.setLogin(cpfLimpo);
        usuario.setSenha(senhaCriptografada);
        usuario.setTipoUsuario((byte) 1); // 1 = Administrador
        usuario.setStatus((byte) 1);
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário criado para administrador. ID: {}", usuarioSalvo.getId());
        
        Administrador administrador = new Administrador();
        administrador.setAdmNome(dados.admNome());
        administrador.setAdmEmail(dados.admEmail());
        administrador.setAdmCodigoAtorizacao(dados.admCodigoAutorizacao());
        administrador.setAdmStatus((byte) 1);
        administrador.setAdmDataCriacao(Date.valueOf(LocalDate.now()));
        administrador.setAdmUsuario(usuarioSalvo);
        
        Administrador administradorSalvo = administradorOutputPort.save(administrador);
        log.info("Administrador cadastrado com sucesso. ID: {}", administradorSalvo.getAdmCodigo());
        
        credenciaisEmailService.enviarCredenciaisAdministrador(dados.admEmail(), dados.admNome(), cpfLimpo, senhaGerada);
        
        return administradorSalvo;
    }
    
    private String gerarSenhaAleatoria() {
        SecureRandom random = new SecureRandom();
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            int index = random.nextInt(CARACTERES_SENHA.length());
            senha.append(CARACTERES_SENHA.charAt(index));
        }
        return senha.toString();
    }
    
    private String limparCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }
}
