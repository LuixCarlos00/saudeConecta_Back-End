package br.com.saudeConecta.config.security;


import br.com.saudeConecta.application.assinatura.AcessoAssinaturaService;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.config.logging.MdcChaves;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.erro.ErrorResponse;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class FiltroAcesso extends OncePerRequestFilter {

    /** Serializa o ErrorResponse igual ao Jackson do Spring (datas ISO, sem nulos). */
    private static final ObjectMapper JSON = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final AcessoAssinaturaService acessoAssinaturaService;

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
                // Sem o token no log: só o tipo da falha (expirado, assinatura, formato) e a rota
                log.warn("auth.acesso negado motivo=JWT_REJEITADO causa={} metodo={} rota={} ip={}", e.getClass().getSimpleName(),
                        request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
                responderErro(request, response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_INVALIDO", "Token invalido ou expirado");
                return;
            }

            if (usuario == null) {
                log.warn("auth.acesso negado motivo=USUARIO_INEXISTENTE login={} metodo={} rota={}",
                        MascaraLog.cpf(subject), request.getMethod(), request.getRequestURI());
                responderErro(request, response, HttpServletResponse.SC_UNAUTHORIZED, "USUARIO_NAO_ENCONTRADO", "Usuario nao encontrado. Faca login novamente.");
                return;
            }

            if (!usuario.isEnabled()) {
                log.warn("auth.acesso negado motivo=USUARIO_BLOQUEADO usuarioId={} metodo={} rota={}",
                        usuario.getId(), request.getMethod(), request.getRequestURI());
                responderErro(request, response, HttpServletResponse.SC_UNAUTHORIZED, "USUARIO_BLOQUEADO", "Usuario bloqueado ou inativo");
                return;
            }

            MDC.put(MdcChaves.USER_ID, String.valueOf(usuario.getId()));
            if (usuario.getOrganizacaoId() != null) {
                MDC.put(MdcChaves.TENANT_ID, String.valueOf(usuario.getOrganizacaoId()));
            }

            if (assinaturaBloqueiaAcesso(usuario, request)) {
                log.warn("auth.acesso negado motivo=ASSINATURA_SEM_ACESSO usuarioId={} organizacaoId={} metodo={} rota={}",
                        usuario.getId(), usuario.getOrganizacaoId(), request.getMethod(), request.getRequestURI());
                responderErro(request, response, HttpServletResponse.SC_FORBIDDEN, "PLANO_SEM_ACESSO",
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

    /**
     * Responde no mesmo formato do GlobalExceptionHandler ({@link ErrorResponse}): o login lê
     * {@code error} no 401 e o ErrorInterceptor do front lê {@code message} no 403 de plano.
     */
    private void responderErro(HttpServletRequest request, HttpServletResponse response, int status, String codigo,
                               String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(ErrorResponse.of(status, codigo, mensagem, request.getRequestURI())));
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
