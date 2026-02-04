package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.EmailRecuperacaoSenhaService;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
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

    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailRecuperacaoSenhaService emailRecuperacaoSenhaService;
    private final PasswordEncoder passwordEncoder;
    private final Executor emailTaskExecutor;

    public HomeService(
            MedicoRepository medicoRepository,
            UsuarioRepository usuarioRepository,
            EmailRecuperacaoSenhaService emailRecuperacaoSenhaService,
            PasswordEncoder passwordEncoder,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.medicoRepository = medicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailRecuperacaoSenhaService = emailRecuperacaoSenhaService;
        this.passwordEncoder = passwordEncoder;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    @Transactional
    public void recuperarSenhaPorEmail(String email) {
        log.info("Iniciando recuperação de senha para email: {}", email);

        // Busca médico pelo email
        Optional<Medico> medicoOpt = medicoRepository.findByMedEmail(email);
        if (medicoOpt.isPresent()) {
            processarRecuperacaoMedico(medicoOpt.get(), email);
            return;
        }

        log.warn("Email não encontrado no sistema: {}", email);
        throw new EmailNaoEncontradoException("Email não encontrado no sistema");
    }

    private void processarRecuperacaoMedico(Medico medico, String email) {
        Usuario usuario = medico.getUsuario();
        String novaSenha = gerarSenhaAleatoria();

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        // Envio de email assíncrono otimizado
        CompletableFuture.runAsync(() -> {
            try {
                emailRecuperacaoSenhaService.enviarEmailRecuperacao(email, medico.getMedNome(), usuario.getLogin(), novaSenha);
                log.info("Email de recuperação enviado para médico: {}", email);
            } catch (Exception e) {
                log.error("Erro ao enviar email de recuperação para médico: {}", email, e);
            }
        }, emailTaskExecutor);

        log.info("Senha atualizada para médico: {}", email);
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
