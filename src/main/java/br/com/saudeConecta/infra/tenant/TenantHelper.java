package br.com.saudeConecta.infra.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantHelper {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    
    public Long getCurrentTenantId() {
        return TenantContext.getCurrentTenantOrThrow();
    }
    
    public Long getCurrentTenantIdOrNull() {
        return TenantContext.getCurrentTenant();
    }
    
    public Long getCurrentUserId() {
        return TenantContext.getCurrentUser();
    }
    
    public boolean hasTenant() {
        return TenantContext.hasTenant();
    }

    /**
     * Indica se o usuário autenticado possui o papel de plataforma (ROOT).
     * A verificação é explícita pelo papel, nunca pela ausência de organização no token.
     *
     * @return true quando a autenticação atual contém ROLE_SUPER_ADMIN
     */
    public boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_SUPER_ADMIN.equals(a.getAuthority()));
    }

    /**
     * Retorna o tenant do token; devolve null (visão global) somente para o SUPER_ADMIN sem organização.
     * Substitui o antigo padrão "orgId == null significa super admin", que falhava aberto para
     * qualquer requisição sem tenant definido.
     *
     * @return ID da organização do token, ou null se o usuário for SUPER_ADMIN sem organização
     * @throws TenantNotDefinedException quando não há tenant e o usuário não é SUPER_ADMIN
     */
    public Long getCurrentTenantIdOrNullIfSuperAdmin() {
        Long tenantId = getCurrentTenantIdOrNull();
        if (tenantId != null) {
            return tenantId;
        }
        if (isSuperAdmin()) {
            return null;
        }
        throw new TenantNotDefinedException("Tenant não definido no contexto atual");
    }
    
    public void validateTenant(Long entityOrganizacaoId) {
        Long currentTenant = getCurrentTenantId();
        if (!currentTenant.equals(entityOrganizacaoId)) {
            throw new TenantAccessDeniedException(
                "Acesso negado: entidade pertence a outra organização"
            );
        }
    }
}
