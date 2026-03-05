package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
public class HomeService {

    private final ProfissionalRepository profissionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecretariaRepository secretariaRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final EmailNotificacaoService emailNotificacaoService;
    private final PasswordEncoder passwordEncoder;

    public HomeService(
            ProfissionalRepository profissionalRepository,
            UsuarioRepository usuarioRepository,
            SecretariaRepository secretariaRepository,
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            EmailNotificacaoService emailNotificacaoService,
            PasswordEncoder passwordEncoder) {
        this.profissionalRepository = profissionalRepository;
        this.usuarioRepository = usuarioRepository;
        this.secretariaRepository = secretariaRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.emailNotificacaoService = emailNotificacaoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void recuperarSenhaPorEmail(String email) {
        log.info("Iniciando recuperação de senha para email: {}", email);

        Usuario usuario = null;
        String nome = null;

        // Busca em Profissional
        Optional<Profissional> profissionalOpt = profissionalRepository.findByEmail(email);
        if (profissionalOpt.isPresent()) {
            Profissional profissional = profissionalOpt.get();
            usuario = profissional.getUsuario();
            nome = profissional.getNome();
        } else {
            // Busca em Secretaria
            Optional<Secretaria> secretariaOpt = secretariaRepository.findByEmail(email);
            if (secretariaOpt.isPresent()) {
                Secretaria secretaria = secretariaOpt.get();
                usuario = secretaria.getUsuario();
                nome = secretaria.getNome();
            } else {
                // Busca em AdminOrganizacao
                Optional<AdminOrganizacao> adminOpt = adminOrganizacaoRepository.findByEmail(email);
                if (adminOpt.isPresent()) {
                    AdminOrganizacao admin = adminOpt.get();
                    usuario = admin.getUsuario();
                    nome = admin.getNome();
                }
            }
        }

        if (usuario != null) {
            processarRecuperacaoUsuario(usuario, nome, email);
            return;
        }

        log.warn("Email não encontrado no sistema: {}", email);
        throw new EmailNaoEncontradoException("Email não encontrado no sistema");
    }

    private void processarRecuperacaoUsuario(Usuario usuario, String nome, String email) {
        String novaSenha = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        Long organizacaoId = usuario.getOrganizacao() != null ? usuario.getOrganizacao().getId() : null;
        emailNotificacaoService.enviarRecuperacaoSenha(email, nome, usuario.getLogin(), novaSenha, organizacaoId);

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
