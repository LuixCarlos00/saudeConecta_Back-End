package br.com.saudeConecta.application.auth;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.notificacao.DadosEmailRecuperacaoSenha;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.TokenRecuperacaoSenha;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.TokenRecuperacaoSenhaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Recuperação de senha por link. O pedido não mexe na senha: gera um token de uso único, guarda
 * só o hash dele e manda o link por e-mail. A senha só muda quando o dono abre o link e escolhe
 * a nova, então digitar o e-mail de outra pessoa na tela não derruba a senha dela.
 *
 * <p>Se o mesmo e-mail estiver em mais de uma conta (ex.: ROOT e um admin de teste), cada conta
 * recebe o próprio link, com o login dela no e-mail.</p>
 */
@Service
@Slf4j
public class RecuperacaoSenhaService {

    /** Código do erro de link inválido; o front usa para trocar a tela pelo aviso de link expirado. */
    public static final String CODIGO_TOKEN_INVALIDO = "TOKEN_RECUPERACAO_INVALIDO";
    static final String MENSAGEM_TOKEN_INVALIDO =
            "Link inválido ou expirado. Solicite uma nova recuperação de senha.";

    /** 32 bytes = 256 bits: inviável de adivinhar por tentativa. */
    private static final int BYTES_TOKEN = 32;
    private static final SecureRandom GERADOR_TOKEN = new SecureRandom();
    private static final String NOME_PADRAO = "Usuário";
    private static final String CAMINHO_TELA_REDEFINICAO = "/#/redefinir-senha?token=";

    private final ProfissionalRepository profissionalRepository;
    private final SecretariaRepository secretariaRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final EmailNotificacaoService emailNotificacaoService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String urlFrontend;
    private final long validadeMinutos;

    public RecuperacaoSenhaService(ProfissionalRepository profissionalRepository,
                                   SecretariaRepository secretariaRepository,
                                   AdminOrganizacaoRepository adminOrganizacaoRepository,
                                   UsuarioRepository usuarioRepository,
                                   TokenRecuperacaoSenhaRepository tokenRepository,
                                   EmailNotificacaoService emailNotificacaoService,
                                   PasswordEncoder passwordEncoder,
                                   Clock clock,
                                   @Value("${app.frontend-url}") String urlFrontend,
                                   @Value("${app.recuperacao-senha.validade-minutos:30}") long validadeMinutos) {
        this.profissionalRepository = profissionalRepository;
        this.secretariaRepository = secretariaRepository;
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.emailNotificacaoService = emailNotificacaoService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        // Sem a barra final, para não gerar "//#/" no link
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
        this.validadeMinutos = validadeMinutos;
    }

    /**
     * Gera um link por conta cadastrada com o e-mail e envia por e-mail. E-mail não cadastrado só
     * vai para o log: quem chama responde sempre a mesma mensagem, para não revelar quais e-mails
     * existem.
     *
     * @param email e-mail informado na tela (já validado como não vazio)
     * @return quantidade de links enviados (0 quando o e-mail não está cadastrado)
     */
    @Transactional
    public int solicitar(String email) {
        String emailNormalizado = email.trim();
        log.info("auth.senha.recuperar inicio email={}", MascaraLog.email(emailNormalizado));

        Map<Long, ContaRecuperavel> contas = buscarContas(emailNormalizado);
        if (contas.isEmpty()) {
            log.warn("auth.senha.recuperar ignorado motivo=EMAIL_NAO_CADASTRADO email={}",
                    MascaraLog.email(emailNormalizado));
            return 0;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        contas.values().forEach(conta -> gerarEEnviarLink(conta, emailNormalizado, agora));

        log.info("auth.senha.recuperar ok email={} contas={}", MascaraLog.email(emailNormalizado), contas.size());
        return contas.size();
    }

    /**
     * Confere se o link ainda vale, para a tela decidir entre o formulário e o aviso de link expirado.
     *
     * @param token token recebido no link
     * @throws BusinessException 400 com código {@value #CODIGO_TOKEN_INVALIDO} quando o link não vale
     */
    @Transactional(readOnly = true)
    public void validar(String token) {
        LocalDateTime agora = LocalDateTime.now(clock);
        TokenRecuperacaoSenha registro = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.isValido(agora))
                .orElseThrow(() -> tokenInvalido("VALIDAR"));
        log.debug("auth.senha.redefinir validar ok usuarioId={}", registro.getUsuario().getId());
    }

    /**
     * Troca a senha do dono do link e consome o token. Os outros links pendentes do mesmo usuário
     * também deixam de valer.
     *
     * @param token     token recebido no link
     * @param novaSenha nova senha em claro (tamanho já validado no DTO)
     * @throws BusinessException 400 com código {@value #CODIGO_TOKEN_INVALIDO} quando o link não vale
     */
    @Transactional
    public void redefinir(String token, String novaSenha) {
        LocalDateTime agora = LocalDateTime.now(clock);
        // Trava a linha: dois envios do mesmo link ao mesmo tempo não trocam a senha duas vezes
        TokenRecuperacaoSenha registro = tokenRepository.findByTokenHashParaUso(hash(token))
                .filter(t -> t.isValido(agora))
                .orElseThrow(() -> tokenInvalido("REDEFINIR"));

        Usuario usuario = registro.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        registro.marcarUsado(agora);
        tokenRepository.saveAndFlush(registro);
        tokenRepository.invalidarPendentes(usuario.getId(), agora);

        log.info("auth.senha.redefinir ok usuarioId={}", usuario.getId());
    }

    private Map<Long, ContaRecuperavel> buscarContas(String email) {
        // LinkedHashMap por id: a mesma conta achada em duas tabelas recebe um link só
        Map<Long, ContaRecuperavel> contas = new LinkedHashMap<>();
        adicionar(contas, profissionalRepository.findAllByEmail(email), Profissional::getUsuario, Profissional::getNome);
        adicionar(contas, secretariaRepository.findAllByEmail(email), Secretaria::getUsuario, Secretaria::getNome);
        adicionar(contas, adminOrganizacaoRepository.findAllByEmail(email), AdminOrganizacao::getUsuario, AdminOrganizacao::getNome);
        usuarioRepository.findByEmailAndOrganizacaoIsNull(email)
                .ifPresent(root -> contas.putIfAbsent(root.getId(), new ContaRecuperavel(root, NOME_PADRAO)));
        return contas;
    }

    private <T> void adicionar(Map<Long, ContaRecuperavel> contas, Iterable<T> registros,
                               Function<T, Usuario> usuarioDe, Function<T, String> nomeDe) {
        for (T registro : registros) {
            Usuario usuario = usuarioDe.apply(registro);
            if (usuario == null) {
                continue;
            }
            String nome = nomeDe.apply(registro);
            contas.putIfAbsent(usuario.getId(),
                    new ContaRecuperavel(usuario, nome == null || nome.isBlank() ? NOME_PADRAO : nome));
        }
    }

    private void gerarEEnviarLink(ContaRecuperavel conta, String email, LocalDateTime agora) {
        Usuario usuario = conta.usuario();
        // Pedido novo substitui o anterior: só o link mais recente funciona
        tokenRepository.invalidarPendentes(usuario.getId(), agora);

        String token = gerarToken();
        tokenRepository.save(new TokenRecuperacaoSenha(usuario, hash(token), agora, agora.plusMinutes(validadeMinutos)));

        Long organizacaoId = usuario.getOrganizacao() != null ? usuario.getOrganizacao().getId() : null;
        emailNotificacaoService.enviarLinkRecuperacaoSenha(new DadosEmailRecuperacaoSenha(
                email, conta.nome(), usuario.getLogin(), urlFrontend + CAMINHO_TELA_REDEFINICAO + token,
                validadeMinutos, organizacaoId));

        log.info("auth.senha.recuperar link gerado usuarioId={} validadeMinutos={}", usuario.getId(), validadeMinutos);
    }

    private BusinessException tokenInvalido(String etapa) {
        log.warn("auth.senha.redefinir negado motivo=TOKEN_INVALIDO etapa={}", etapa);
        return new BusinessException(MENSAGEM_TOKEN_INVALIDO, HttpStatus.BAD_REQUEST, CODIGO_TOKEN_INVALIDO);
    }

    private static String gerarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        GERADOR_TOKEN.nextBytes(bytes);
        // URL-safe e sem "=": vai direto na query string do link
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 em hex. Sem salt de propósito: o token já tem 256 bits aleatórios, e o hash
     * determinístico permite achar o registro por índice.
     */
    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    private record ContaRecuperavel(Usuario usuario, String nome) {
    }
}
