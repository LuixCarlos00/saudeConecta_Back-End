package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailRecuperacaoSenhaService;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
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
public class HomeService {

    private final ProfissionalRepository profissionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailRecuperacaoSenhaService emailRecuperacaoSenhaService;
    private final PasswordEncoder passwordEncoder;
    private final Executor emailTaskExecutor;

    public HomeService(
            ProfissionalRepository profissionalRepository,
            UsuarioRepository usuarioRepository,
            EmailRecuperacaoSenhaService emailRecuperacaoSenhaService,
            PasswordEncoder passwordEncoder,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.profissionalRepository = profissionalRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailRecuperacaoSenhaService = emailRecuperacaoSenhaService;
        this.passwordEncoder = passwordEncoder;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    @Transactional
    public void recuperarSenhaPorEmail(String email) {
        log.info("Iniciando recuperação de senha para email: {}", email);

        // Busca usuário pelo login (email)
        Usuario usuario = usuarioRepository.findUsuarioByLogin(email);
        if (usuario != null) {
            processarRecuperacaoUsuario(usuario, email);
            return;
        }

        log.warn("Email não encontrado no sistema: {}", email);
        throw new EmailNaoEncontradoException("Email não encontrado no sistema");
    }

    private void processarRecuperacaoUsuario(Usuario usuario, String email) {
        String novaSenha = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        // Busca nome do profissional se existir
        Optional<Profissional> profissionalOpt = profissionalRepository.findByUsuario_Id(usuario.getId());
        String nome = profissionalOpt.map(Profissional::getNome).orElse("Usuário");

        // Envio de email assíncrono
        CompletableFuture.runAsync(() -> {
            try {
                emailRecuperacaoSenhaService.enviarEmailRecuperacao(email, nome, usuario.getLogin(), novaSenha);
                log.info("Email de recuperação enviado para: {}", email);
            } catch (Exception e) {
                log.error("Erro ao enviar email de recuperação para: {}", email, e);
            }
        }, emailTaskExecutor);

        log.info("Senha atualizada para: {}", email);
    }

    private String gerarSenhaAleatoria() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            senha.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return senha.toString();
    }

    public static class EmailNaoEncontradoException extends RuntimeException {
        public EmailNaoEncontradoException(String message) {
            super(message);
        }
    }
}
