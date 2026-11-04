package br.com.saudeConecta.infra.configuracoesseguranca.filtroSeguranca;


import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.service.AcessoAssinaturaService;
import br.com.saudeConecta.util.TokenLogUtil;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class FiltroAcesso extends OncePerRequestFilter {

    /**
     * Rotas que a clínica com assinatura suspensa/cancelada ainda precisa para regularizar o
     * pagamento (tela "Minha assinatura" do front). Todas são escopadas ao tenant do token.
     */
    private static final List<String> ROTAS_LIBERADAS_SEM_ASSINATURA = List.of(
            "/assinaturas/minha-assinatura",
            "/assinaturas/limites",
            "/cobrancas/minhas-cobrancas",
            "/cobrancas/pendente-atual"
    );

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AcessoAssinaturaService acessoAssinaturaService;

    /**
     * Autentica a requisição pelo JWT e revalida, a cada chamada, o que pode ter mudado depois
     * da emissão do token: usuário apagado ou bloqueado (401) e assinatura da clínica sem
     * acesso (403). Sem isso o token continuaria valendo até expirar (24h).
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        var tokenJwt = recuperarToken(request);

        if (tokenJwt != null) {
            String subject;
            Usuario usuario;
            try {
                subject = tokenService.getSubject(tokenJwt);
                usuario = (Usuario) usuarioRepository.findByLogin(subject);
            } catch (JWTVerificationException e) {
                responderErro(response, HttpServletResponse.SC_UNAUTHORIZED, "error", "Token invalido ou expirado");
                return;
            }

            if (usuario == null) {
                log.warn("Token valido para usuario inexistente (login {}) em {} {}",
                        TokenLogUtil.mascarar(subject), request.getMethod(), request.getRequestURI());
                responderErro(response, HttpServletResponse.SC_UNAUTHORIZED, "error", "Usuario nao encontrado. Faca login novamente.");
                return;
            }

            if (!usuario.isEnabled()) {
                log.warn("Token de usuario bloqueado (id {}) em {} {}",
                        usuario.getId(), request.getMethod(), request.getRequestURI());
                responderErro(response, HttpServletResponse.SC_UNAUTHORIZED, "error", "Usuario bloqueado ou inativo");
                return;
            }

            if (assinaturaBloqueiaAcesso(usuario, request)) {
                log.warn("Acesso negado ao usuario id {}: assinatura da organizacao {} sem acesso ({} {})",
                        usuario.getId(), usuario.getOrganizacaoId(), request.getMethod(), request.getRequestURI());
                responderErro(response, HttpServletResponse.SC_FORBIDDEN, "message",
                        "O plano da sua organizacao esta suspenso, cancelado ou inexistente. Regularize a assinatura para continuar.");
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        filterChain.doFilter(request,response);
    }

    /**
     * ROOT não depende de assinatura; rotas de pagamento seguem liberadas para a clínica
     * conseguir sair da suspensão.
     */
    private boolean assinaturaBloqueiaAcesso(Usuario usuario, HttpServletRequest request) {
        Long organizacaoId = usuario.getOrganizacaoId();
        if (usuario.isRoot() || organizacaoId == null) {
            return false;
        }
        String uri = request.getRequestURI();
        if (ROTAS_LIBERADAS_SEM_ASSINATURA.stream().anyMatch(uri::startsWith)) {
            return false;
        }
        return !acessoAssinaturaService.organizacaoPermiteAcesso(organizacaoId);
    }

    private void responderErro(HttpServletResponse response, int status, String campo, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"" + campo + "\": \"" + mensagem + "\"}");
    }


    private @Nullable String recuperarToken(@NotNull HttpServletRequest request) {

        var authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.replace("Bearer ", "");
        }

       // a requisisao ta vindo so o bearer  validar se tem token e nao o bearer sozinho   fazer isso no metodo de login front tbm  se e necessario colocar o bearer
        return null;
    }


}
