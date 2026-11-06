package br.com.saudeConecta.application.usuario;

import br.com.saudeConecta.config.logging.MascaraLog;

import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
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

    /** Um gerador só por classe: criar um SecureRandom por senha gasta entropia à toa (S2119). */
    private static final SecureRandom GERADOR_SENHA = new SecureRandom();

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
        log.info("auth.senha.recuperar inicio email={}", MascaraLog.email(email));

        Usuario usuario = null;

        // Busca em Profissional
        Optional<Profissional> profissionalOpt = profissionalRepository.findByEmail(email);
        if (profissionalOpt.isPresent()) {
            Profissional profissional = profissionalOpt.get();
            usuario = profissional.getUsuario();
        } else {
            // Busca em Secretaria
            Optional<Secretaria> secretariaOpt = secretariaRepository.findByEmail(email);
            if (secretariaOpt.isPresent()) {
                Secretaria secretaria = secretariaOpt.get();
                usuario = secretaria.getUsuario();
            } else {
                // Busca em AdminOrganizacao
                Optional<AdminOrganizacao> adminOpt = adminOrganizacaoRepository.findByEmail(email);
                if (adminOpt.isPresent()) {
                    AdminOrganizacao admin = adminOpt.get();
                    usuario = admin.getUsuario();
                } else {
                    // Busca na tabela usuarios pelo campo email (para usuarios sem organizacao, ex: SUPER_ADMIN)
                    Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndOrganizacaoIsNull(email);
                    if (usuarioOpt.isPresent()) {
                        usuario = usuarioOpt.get();
                        log.debug("auth.senha.recuperar usuario sem organizacao email={}", MascaraLog.email(email));
                    }
                }
            }
        }

        if (usuario != null) {
            processarRecuperacaoUsuario(usuario , email);
            return;
        }

        log.warn("auth.senha.recuperar negado motivo=EMAIL_NAO_CADASTRADO email={}", MascaraLog.email(email));
        throw new EmailNaoEncontradoException("Email nao encontrado no sistema");
    }

    private void processarRecuperacaoUsuario(Usuario usuario, String email) {
        String novaSenha = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        Long organizacaoId = usuario.getOrganizacao() != null ? usuario.getOrganizacao().getId() : null;
        emailNotificacaoService.enviarRecuperacaoSenha(email, "Usuário", usuario.getLogin(), novaSenha, organizacaoId);

        log.info("auth.senha.recuperar ok email={}", MascaraLog.email(email));
    }

    private String gerarSenhaAleatoria() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            senha.append(caracteres.charAt(GERADOR_SENHA.nextInt(caracteres.length())));
        }
        return senha.toString();
    }

    public static class EmailNaoEncontradoException extends RuntimeException {
        public EmailNaoEncontradoException(String message) {
            super(message);
        }
    }
}
