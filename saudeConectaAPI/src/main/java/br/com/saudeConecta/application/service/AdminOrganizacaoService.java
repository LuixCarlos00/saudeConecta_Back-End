package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.CredenciaisEmailService;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class AdminOrganizacaoService {

    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredenciaisEmailService credenciaisEmailService;
    private final Executor emailTaskExecutor;

    public AdminOrganizacaoService(
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            CredenciaisEmailService credenciaisEmailService,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.credenciaisEmailService = credenciaisEmailService;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    @Transactional(readOnly = true)
    public Optional<AdminOrganizacao> buscarPorId(Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return adminOrganizacaoRepository.findById(id);
    }

    @Transactional
    public AdminOrganizacao cadastrar(CadastrarAdminRequest request, Long organizacaoId) {
        log.info("Cadastrando administrador: {} para organização ID: {}", request.admNome(), organizacaoId);

        // Verifica se CPF já existe como login
        if (usuarioRepository.existsByLogin(request.admCpf())) {
            log.warn("CPF já cadastrado como login: {}", request.admCpf());
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        // Busca a organização
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
            .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        // Cria o usuário com CPF como login e senha aleatória
        String cpfLimpo = limparCpf(request.admCpf());
        String senhaGerada = gerarSenhaAleatoria();
        
        Usuario usuario = new Usuario();
        usuario.setLogin(cpfLimpo);
        usuario.setSenha(passwordEncoder.encode(senhaGerada));
        usuario.setTipoUsuario((byte) 1); // ADMIN_ORG
        usuario.setStatus((byte) 1); // ATIVO
        usuario.setOrganizacao(organizacao);
        

        // Cria o AdminOrganizacao
        AdminOrganizacao admin = AdminOrganizacao.builder()
            .organizacao(organizacao)
            .usuario(usuario)
            .nome(request.admNome())
            .email(request.admEmail())
            .cargo("Administrador")
            .isOwner(false)
            .status(AdminOrganizacao.StatusAdmin.ATIVO)
            .build();

        usuario = usuarioRepository.save(usuario);
        log.debug("Usuário criado com ID: {}", usuario.getId());
        admin = adminOrganizacaoRepository.save(admin);
        log.info("Administrador cadastrado com sucesso. ID: {}", admin.getId());

        // Envio de email assíncrono otimizado
        CompletableFuture.runAsync(() -> {
            try {
                credenciaisEmailService.enviarCredenciaisAdministrador(
                    request.admEmail(), 
                    request.admNome(), 
                    cpfLimpo, 
                    senhaGerada
                );
                log.info("Email enviado para: {}", request.admEmail());
            } catch (Exception e) {
                log.error("Erro ao enviar email para: {}", request.admEmail(), e);
            }
        }, emailTaskExecutor);

        return admin;
    }

    @Transactional
    public AdminOrganizacao atualizarAdmByOrg(Long id, String nome, String email) {
        log.info("Atualizando administrador ID: {}", id);

        AdminOrganizacao admin = adminOrganizacaoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));

        if (nome != null && !nome.isBlank()) {
            admin.setNome(nome);
        }
        if (email != null && !email.isBlank()) {
            admin.setEmail(email);
        }

        admin = adminOrganizacaoRepository.save(admin);
        log.info("Administrador atualizado com sucesso. ID: {}", admin.getId());

        return admin;
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
