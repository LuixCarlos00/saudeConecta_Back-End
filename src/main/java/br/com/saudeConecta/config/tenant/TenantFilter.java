package br.com.saudeConecta.config.tenant;

import br.com.saudeConecta.config.logging.MdcChaves;
import br.com.saudeConecta.config.security.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class TenantFilter extends OncePerRequestFilter {
    
    private final TokenService tokenService;
    
    // Espelha as rotas permitAll de SecurityConfigurations; só entram paths com handler real
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/Home/login",
        "/Home/esqueciMinhaSenha",
        "/cep",
        "/publico",
        "/planos"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String path = request.getRequestURI();
            
            if (isPublicPath(path)) {
                log.debug("tenant.filtro ignorado motivo=PATH_PUBLICO path={}", path);
                filterChain.doFilter(request, response);
                return;
            }
            
            String token = extractToken(request);
            
            if (token != null) {
                try {
                    Long organizacaoId = tokenService.getOrganizacaoIdFromToken(token);
                    Long userId = tokenService.getUserIdFromToken(token);
                    
                    if (organizacaoId != null) {
                        TenantContext.setCurrentTenant(organizacaoId);
                        MDC.put(MdcChaves.TENANT_ID, String.valueOf(organizacaoId));
                        log.debug("tenant.filtro definido organizacaoId={} usuarioId={}", organizacaoId, userId);
                    }
                    
                    if (userId != null) {
                        TenantContext.setCurrentUser(userId);
                        MDC.put(MdcChaves.USER_ID, String.valueOf(userId));
                    }
                } catch (Exception e) {
                    log.warn("tenant.filtro falha motivo=ERRO_EXTRAIR_TENANT", e);
                }
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            TenantContext.clear();
            MDC.remove(MdcChaves.TENANT_ID);
            MDC.remove(MdcChaves.USER_ID);
        }
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
