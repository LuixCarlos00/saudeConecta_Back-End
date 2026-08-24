package br.com.saudeConecta.infra.tenant;

import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/Home/login",
        "/Home/cadastralogin",
        "/Home/esqueciMinhaSenha",
        "/Home/trocaDeSenha",
        "/Home/buscarUsuarioExistente",
        "/Home/recuperaLogin",
        "/cep",
        "/paciente/buscarPorEmail",
        "/paciente/InserirCodigo",
        "/medico/buscarPorEmail",
        "/public",
        "/publico",
        "/planos",
        "/swagger-ui",
        "/v3/api-docs",
        "/actuator"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String path = request.getRequestURI();
            
            if (isPublicPath(path)) {
                log.debug("Path público, pulando filtro de tenant: {}", path);
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
                        log.debug("Tenant definido: {} para usuário: {}", organizacaoId, userId);
                    }
                    
                    if (userId != null) {
                        TenantContext.setCurrentUser(userId);
                    }
                } catch (Exception e) {
                    log.warn("Erro ao extrair tenant do token: {}", e.getMessage());
                }
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            TenantContext.clear();
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
