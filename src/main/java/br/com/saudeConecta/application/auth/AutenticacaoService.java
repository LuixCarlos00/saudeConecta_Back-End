package br.com.saudeConecta.application.auth;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.application.usuario.HomeService;
import br.com.saudeConecta.application.usuario.dto.DadosLoginUsuario;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.config.security.TokenService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.AuditoriaAcessoMaster;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.AuditoriaAcessoMasterRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Regra do login e da recuperação de senha (antes dentro do {@code HomeController}, fase B6).
 *
 * <p><b>Sem {@code @Transactional} de propósito</b>, como era no controller: a gravação da
 * auditoria da senha master roda na transação do próprio repository, e a falha dela é capturada
 * aqui para negar o acesso master. Uma transação em volta faria essa falha marcar tudo como
 * rollback-only; da mesma forma, a {@link HomeService.EmailNaoEncontradoException} sai da
 * transação do {@link HomeService} e é engolida aqui sem efeito colateral.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutenticacaoService {
    private static final String NOME_PADRAO_PROFISSIONAL = "Profissional";

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

    /**
     * Autentica o usuário (senha master do ROOT ou senha própria), confere o perfil escolhido na
     * tela e o plano da clínica, e gera o token JWT.
     *
     * @param dados login, senha e perfil escolhido (opcional)
     * @param ip    IP do cliente, gravado na auditoria quando a senha master é usada
     * @return token ou recusa com mensagem
     * @throws org.springframework.security.core.AuthenticationException credenciais inválidas ou
     *         usuário bloqueado (o handler global responde 401)
     */
    public ResultadoLogin autenticar(DadosLoginUsuario dados, String ip) {
        // CPF/CNPJ pode vir com máscara; o login gravado é só dígitos
        String login = LoginNormalizado.normalizar(dados.login());

        // Tenta autenticar via senha master (SUPER_ADMIN impersonando outro usuario)
        Usuario usuarioMaster = autenticarComSenhaMaster(login, dados.senha(), ip);
        if (usuarioMaster != null) {
            return gerarResultadoLogin(usuarioMaster);
        }

        // Fluxo normal de autenticacao
        var authenticatetoken = new UsernamePasswordAuthenticationToken(login, dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        Usuario usuario = (Usuario) authentication.getPrincipal();

        // Validar tipo de usuário selecionado no login
        if (dados.perfil() != null && !dados.perfil().isBlank()) {
            try {
                TipoUsuarioNovo tipoSelecionado = TipoUsuarioNovo.valueOf(dados.perfil().toUpperCase());
                if (!tipoSelecionado.equals(usuario.getTipoUsuarioNovo())) {
                    log.warn("auth.login negado motivo=TIPO_DIVERGENTE usuarioId={} tipoSelecionado={} tipoUsuario={}",
                            usuario.getId(), tipoSelecionado, usuario.getTipoUsuarioNovo());
                    return ResultadoLogin.recusado(ResultadoLogin.Recusa.TIPO_DIVERGENTE,
                            "O tipo selecionado não corresponde ao tipo do usuário no sistema");
                }
            } catch (IllegalArgumentException e) {
                log.warn("auth.login negado motivo=TIPO_INVALIDO perfil={}", dados.perfil());
                return ResultadoLogin.recusado(ResultadoLogin.Recusa.TIPO_INVALIDO, "Tipo inválido");
            }
        }

        return gerarResultadoLogin(usuario);
    }

    /**
     * Pede o envio de nova senha por e-mail. Não informa se o e-mail existe: o "não encontrado" é
     * só registrado no log, para o controller responder sempre a mesma mensagem.
     *
     * @param email e-mail informado (já validado como não vazio)
     */
    public void solicitarRecuperacaoSenha(String email) {
        try {
            recuperacaoSenhaService.recuperarSenhaPorEmail(email);
        } catch (HomeService.EmailNaoEncontradoException e) {
            log.info("auth.senha.recuperar ignorado motivo=RESPOSTA_GENERICA email={}", MascaraLog.email(email));
        }
    }

    /**
     * Verifica se a senha informada e a senha do SUPER_ADMIN (usuario sem organizacao).
     * Se sim, retorna o Usuario alvo (identificado pelo login) para impersonacao.
     * Se nao, retorna null e o fluxo normal de autenticacao segue.
     * Recurso INTENCIONAL de suporte: todo uso fica registrado em auditoria_acesso_master; se a
     * auditoria nao puder ser gravada o acesso master e negado (sem rastro, sem acesso).
     * Alvo bloqueado (INATIVO) e recusado como no fluxo normal, antes da auditoria e do token.
     *
     * @param loginAlvo      login do usuario que se deseja acessar
     * @param senhaInformada senha digitada no login
     * @param ip             IP do cliente (para a auditoria)
     * @return Usuario alvo se a senha master for valida, null caso contrario
     * @throws DisabledException quando o alvo esta bloqueado (o handler global responde 401)
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

            // Mesmo bloqueio do fluxo normal (DisabledException -> 401 "Usuario bloqueado ou inativo"):
            // sem auditoria e sem token para usuario INATIVO.
            if (!usuarioAlvo.isEnabled()) {
                log.warn("auth.senhaMaster negado motivo=USUARIO_BLOQUEADO usuarioAlvoId={}", usuarioAlvo.getId());
                throw new DisabledException("Usuario bloqueado");
            }

            auditoriaAcessoMasterRepository.save(AuditoriaAcessoMaster.builder()
                    .atorUsuarioId(root.getId())
                    .atorLogin(root.getLogin())
                    .alvoUsuarioId(usuarioAlvo.getId())
                    .alvoLogin(usuarioAlvo.getLogin())
                    .alvoOrganizacaoId(usuarioAlvo.getOrganizacaoId())
                    .ip(ip)
                    .dataHora(LocalDateTime.now(FusoHorario.ZONA))
                    .build());
            log.info("auth.senhaMaster ok usuarioAlvoId={} auditado=true", usuarioAlvo.getId());
            return usuarioAlvo;

        } catch (DisabledException e) {
            throw e;
        } catch (Exception e) {
            log.error("auth.senhaMaster falha motivo=ERRO_AUDITORIA acao=ACESSO_NEGADO", e);
            return null;
        }
    }

    /**
     * Confere o plano da clínica e gera o token JWT do usuário já autenticado.
     *
     * @param usuario Usuario autenticado
     * @return token, ou recusa quando a clínica não tem plano que permita o acesso
     */
    private ResultadoLogin gerarResultadoLogin(Usuario usuario) {
        Long organizacaoId = usuario.getOrganizacaoId();

        if (!usuario.isRoot() && organizacaoId != null) {
            Optional<AssinaturaTenant> assinatura = assinaturaTenantRepository
                    .findAssinaturaVigenteByOrganizacaoId(organizacaoId);

            if (assinatura.isEmpty()) {
                log.warn("auth.login negado motivo=SEM_PLANO_ATIVO usuarioId={} organizacaoId={}", usuario.getId(), organizacaoId);
                return ResultadoLogin.recusado(ResultadoLogin.Recusa.PLANO_SEM_ACESSO,
                        "Sua organizacao nao possui um plano ativo. Entre em contato com o administrador.");
            }

            // SUSPENSA recebe o token para conseguir pagar: o FiltroAcesso limita esse token às
            // rotas de assinatura/pagamento e devolve 403 PLANO_SEM_ACESSO no resto.
            if (StatusAssinatura.SUSPENSA.equals(assinatura.get().getStatus())) {
                log.info("auth.login restrito motivo=PLANO_SUSPENSO usuarioId={} organizacaoId={} acesso=SO_PAGAMENTO",
                        usuario.getId(), organizacaoId);
            } else if (!assinatura.get().permiteAcesso()) {
                String statusPlano = assinatura.get().getStatus().getDescricao();
                log.warn("auth.login negado motivo=PLANO_SEM_ACESSO usuarioId={} statusPlano={} organizacaoId={}", usuario.getId(), statusPlano, organizacaoId);
                return ResultadoLogin.recusado(ResultadoLogin.Recusa.PLANO_SEM_ACESSO,
                        "O plano da sua organizacao esta " + statusPlano.toLowerCase() + ". Entre em contato com o administrador.");
            }
        }

        String nomeUsuario = getNomeUsuario(usuario);
        String tipoProfissional = usuario.isClinico() ? getProfissional(usuario) : null;
        String tipoUsuario = usuario.getTipoUsuarioNovo() != null ? usuario.getTipoUsuarioNovo().name() : "GESTOR";
        String tokenJWT = tokenService.gerarToken(usuario, organizacaoId, nomeUsuario, tipoUsuario, tipoProfissional);
        log.info("auth.login ok usuarioId={} organizacaoId={} tipo={} tipoProfissional={}",
                usuario.getId(), organizacaoId, tipoUsuario, tipoProfissional);

        return ResultadoLogin.autenticado(tokenJWT);
    }

    private String getNomeUsuario(Usuario usuario) {
        try {
            // Busca baseada no tipo de usuário
            if (usuario.isGestor()) {
                Optional<AdminOrganizacao> admin = adminOrganizacaoRepository.findByUsuario_Id(usuario.getId());
                return admin.map(AdminOrganizacao::getNome).orElse("Administrador");
            } else if (usuario.isClinico()) {
                Optional<Profissional> profissional = profissionalRepository.findByUsuario_Id(usuario.getId());
                return profissional.map(Profissional::getNome).orElse(NOME_PADRAO_PROFISSIONAL);
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
                return profissional.map(this::descreverTipoProfissional).orElse(NOME_PADRAO_PROFISSIONAL);
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

    /** Tipo do profissional para o token: MEDICO, DENTISTA, o código do tipo ou o padrão se não houver tipo. */
    private String descreverTipoProfissional(Profissional p) {
        if (p.getTipoProfissional() == null) {
            return NOME_PADRAO_PROFISSIONAL;
        }
        if (p.getTipoProfissional().isMedico()) {
            return "MEDICO";
        }
        if (p.getTipoProfissional().isDentista()) {
            return "DENTISTA";
        }
        return p.getTipoProfissional().getCodigo();
    }
}
