package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.config.logging.MascaraLog;

import br.com.saudeConecta.application.usuario.HomeService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.AuditoriaAcessoMaster;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AuditoriaAcessoMasterRepository;
import jakarta.servlet.http.HttpServletRequest;
import br.com.saudeConecta.config.security.TokenService;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.saudeConecta.application.usuario.dto.DadosLoginUsuario;
import br.com.saudeConecta.application.usuario.dto.DadosTokenJWT;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/Home")
@RequiredArgsConstructor
@Slf4j
@Description("Endpoints para autenticação principal do sistema")
public class HomeController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final HomeService recuperacaoSenhaService;
    private final ProfissionalRepository profissionalRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final SecretariaRepository secretariaRepository;
    private final AssinaturaTenantRepository assinaturaTenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaAcessoMasterRepository auditoriaAcessoMasterRepository;

    /** Mesma resposta para e-mail cadastrado ou não: impede descobrir quais e-mails existem. */
    static final String MENSAGEM_RECUPERACAO_GENERICA =
            "Se o e-mail estiver cadastrado, voce recebera as instrucoes para recuperar a senha.";

    @PostMapping("/login")
    @Description("Realiza autenticação do usuário e retorna token JWT. Utilizado em: LoginComponent, AuthService")
    public ResponseEntity<?> autenticar(@RequestBody @NotNull DadosLoginUsuario dados, HttpServletRequest request) {
        // Tenta autenticar via senha master (SUPER_ADMIN impersonando outro usuario)
        Usuario usuarioMaster = autenticarComSenhaMaster(dados.login(), dados.senha(), request.getRemoteAddr());
        if (usuarioMaster != null) {
            return gerarRespostaLogin(usuarioMaster);
        }

        // Fluxo normal de autenticacao
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        Usuario usuario = (Usuario) authentication.getPrincipal();

        // Validar tipo de usuário selecionado no login
        if (dados.perfil() != null && !dados.perfil().isBlank()) {
            try {
                TipoUsuarioNovo tipoSelecionado = TipoUsuarioNovo.valueOf(dados.perfil().toUpperCase());
                if (!tipoSelecionado.equals(usuario.getTipoUsuarioNovo())) {
                    log.warn("auth.login negado motivo=TIPO_DIVERGENTE usuarioId={} tipoSelecionado={} tipoUsuario={}",
                            usuario.getId(), tipoSelecionado, usuario.getTipoUsuarioNovo());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "O tipo selecionado não corresponde ao tipo do usuário no sistema"));
                }
            } catch (IllegalArgumentException e) {
                log.warn("auth.login negado motivo=TIPO_INVALIDO perfil={}", dados.perfil());
                return ResponseEntity.badRequest().body(Map.of("message", "Tipo inválido"));
            }
        }

        return gerarRespostaLogin(usuario);
    }

    /**
     * Verifica se a senha informada e a senha do SUPER_ADMIN (usuario sem organizacao).
     * Se sim, retorna o Usuario alvo (identificado pelo login) para impersonacao.
     * Se nao, retorna null e o fluxo normal de autenticacao segue.
     * Recurso INTENCIONAL de suporte: todo uso fica registrado em auditoria_acesso_master; se a
     * auditoria nao puder ser gravada o acesso master e negado (sem rastro, sem acesso).
     *
     * @param loginAlvo      login do usuario que se deseja acessar
     * @param senhaInformada senha digitada no login
     * @param ip             IP do cliente (para a auditoria)
     * @return Usuario alvo se a senha master for valida, null caso contrario
     */
    private Usuario autenticarComSenhaMaster(String loginAlvo, String senhaInformada, String ip) {
        try {
            Usuario root = usuarioRepository.findByTipoUsuarioNovoAndOrganizacaoIsNull(
                    TipoUsuarioNovo.ROOT);

            if (root == null) {
                return null;
            }

            if (!passwordEncoder.matches(senhaInformada, root.getSenha())) {
                return null;
            }

            // Nao permite impersonar o proprio ROOT via senha master
            if (loginAlvo.equals(root.getLogin())) {
                return null;
            }

            Usuario usuarioAlvo = usuarioRepository.findUsuarioByLogin(loginAlvo);
            if (usuarioAlvo == null) {
                log.warn("auth.senhaMaster negado motivo=LOGIN_INEXISTENTE login={}", MascaraLog.cpf(loginAlvo));
                return null;
            }

            auditoriaAcessoMasterRepository.save(AuditoriaAcessoMaster.builder()
                    .atorUsuarioId(root.getId())
                    .atorLogin(root.getLogin())
                    .alvoUsuarioId(usuarioAlvo.getId())
                    .alvoLogin(usuarioAlvo.getLogin())
                    .alvoOrganizacaoId(usuarioAlvo.getOrganizacaoId())
                    .ip(ip)
                    .dataHora(LocalDateTime.now())
                    .build());
            log.info("auth.senhaMaster ok usuarioAlvoId={} auditado=true", usuarioAlvo.getId());
            return usuarioAlvo;

        } catch (Exception e) {
            log.error("auth.senhaMaster falha motivo=ERRO_AUDITORIA acao=ACESSO_NEGADO", e);
            return null;
        }
    }

    /**
     * Gera a resposta de login para um usuario, incluindo validacao de plano e token JWT.
     *
     * @param usuario Usuario autenticado
     * @return ResponseEntity com token JWT ou erro de plano
     */
    private ResponseEntity<?> gerarRespostaLogin(Usuario usuario) {
        Long organizacaoId = usuario.getOrganizacaoId();

        if (!usuario.isRoot() && organizacaoId != null) {
            Optional<AssinaturaTenant> assinatura = assinaturaTenantRepository
                    .findAssinaturaVigenteByOrganizacaoId(organizacaoId);

            if (assinatura.isEmpty()) {
                log.warn("auth.login negado motivo=SEM_PLANO_ATIVO usuarioId={} organizacaoId={}", usuario.getId(), organizacaoId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Sua organizacao nao possui um plano ativo. Entre em contato com o administrador."));
            }

            // SUSPENSA recebe o token para conseguir pagar: o FiltroAcesso limita esse token às
            // rotas de assinatura/pagamento e devolve 403 PLANO_SEM_ACESSO no resto.
            if (StatusAssinatura.SUSPENSA.equals(assinatura.get().getStatus())) {
                log.info("auth.login restrito motivo=PLANO_SUSPENSO usuarioId={} organizacaoId={} acesso=SO_PAGAMENTO",
                        usuario.getId(), organizacaoId);
            } else if (!assinatura.get().permiteAcesso()) {
                String statusPlano = assinatura.get().getStatus().getDescricao();
                log.warn("auth.login negado motivo=PLANO_SEM_ACESSO usuarioId={} statusPlano={} organizacaoId={}", usuario.getId(), statusPlano, organizacaoId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "O plano da sua organizacao esta " + statusPlano.toLowerCase() + ". Entre em contato com o administrador."));
            }
        }

        String nomeUsuario = getNomeUsuario(usuario);
        String tipoProfissional = usuario.isClinico() ? getProfissional(usuario) : null;
        String tipoUsuario = usuario.getTipoUsuarioNovo() != null ? usuario.getTipoUsuarioNovo().name() : "GESTOR";
        String tokenJWT = tokenService.gerarToken(usuario, organizacaoId, nomeUsuario, tipoUsuario, tipoProfissional);
        log.info("auth.login ok usuarioId={} organizacaoId={} tipo={} tipoProfissional={}",
                usuario.getId(), organizacaoId, tipoUsuario, tipoProfissional);

        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
    }

    /**
     * Gera nova senha e envia por e-mail. Responde SEMPRE 200 com a mesma mensagem, exista ou não o
     * e-mail, para não permitir enumeração de contas. Falha inesperada sobe para o handler global (500).
     *
     * @param dados corpo com o campo "email"
     * @return 200 com mensagem genérica, ou 400 quando o e-mail não foi informado
     */
    @PostMapping("/esqueciMinhaSenha")
    @Description("Recupera senha do usuário enviando nova senha por email")
    public ResponseEntity<?> esqueciMinhaSenha(@RequestBody Map<String, String> dados) {
        String email = dados.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe o e-mail"));
        }

        try {
            recuperacaoSenhaService.recuperarSenhaPorEmail(email);
        } catch (HomeService.EmailNaoEncontradoException e) {
            log.info("auth.senha.recuperar ignorado motivo=RESPOSTA_GENERICA email={}", MascaraLog.email(email));
        }
        return ResponseEntity.ok(Map.of("message", MENSAGEM_RECUPERACAO_GENERICA));
    }

    private String getNomeUsuario(Usuario usuario) {
        try {
            // Busca baseada no tipo de usuário
            if (usuario.isGestor()) {
                Optional<AdminOrganizacao> admin = adminOrganizacaoRepository.findByUsuario_Id(usuario.getId());
                return admin.map(AdminOrganizacao::getNome).orElse("Administrador");
            } else if (usuario.isClinico()) {
                Optional<Profissional> profissional = profissionalRepository.findByUsuario_Id(usuario.getId());
                return profissional.map(Profissional::getNome).orElse("Profissional");
            } else if (usuario.isAssistente()) {
                Optional<Secretaria> secretaria = secretariaRepository.findByUsuario_Id(usuario.getId());
                return secretaria.map(Secretaria::getNome).orElse("Secretária");
            }
        } catch (Exception e) {
            log.warn("auth.login falha motivo=NOME_INDISPONIVEL usuarioId={}", usuario.getId(), e);
        }

        return "Usuario"; // Default fallback
    }

    private String getProfissional(Usuario usuario) {
        try {
            if (usuario.isClinico()) {
                Optional<Profissional> profissional = profissionalRepository.findByUsuarioIdWithRelations(usuario.getId());
                return profissional.map(p -> {
                    if (p.getTipoProfissional() != null) {
                        if (p.getTipoProfissional().isMedico()) {
                            return "MEDICO";
                        } else if (p.getTipoProfissional().isDentista()) {
                            return "DENTISTA";
                        } else {
                            return p.getTipoProfissional().getCodigo();
                        }
                    }
                    return "Profissional";
                }).orElse("Profissional");
            }
            if (usuario.isGestor()) {
                Optional<AdminOrganizacao> admin = adminOrganizacaoRepository.findByUsuario_Id(usuario.getId());
                return admin.isPresent() ? "Administrador" : "null";
            }
            if (usuario.isAssistente()) {
                Optional<Secretaria> secretaria = secretariaRepository.findByUsuario_Id(usuario.getId());
                return secretaria.isPresent() ? "Recepcionista" : "null";
            }
        } catch (Exception e) {
            log.warn("auth.login falha motivo=PROFISSIONAL_INDISPONIVEL usuarioId={}", usuario.getId(), e);
        }

        return "false"; // Default fallback
    }


}
