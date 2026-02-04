package br.com.saudeConecta.infra.tenant;

import org.springframework.stereotype.Component;

@Component
public class TenantHelper {
    
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
    
    public void validateTenant(Long entityOrganizacaoId) {
        Long currentTenant = getCurrentTenantId();
        if (!currentTenant.equals(entityOrganizacaoId)) {
            throw new TenantAccessDeniedException(
                "Acesso negado: entidade pertence a outra organização"
            );
        }
    }
}
