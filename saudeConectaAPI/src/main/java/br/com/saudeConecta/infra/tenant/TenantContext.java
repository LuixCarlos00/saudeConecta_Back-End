package br.com.saudeConecta.infra.tenant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {
    
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    
    private TenantContext() {
    }
    
    public static void setCurrentTenant(Long organizacaoId) {
        log.debug("Definindo tenant atual: {}", organizacaoId);
        CURRENT_TENANT.set(organizacaoId);
    }
    
    public static Long getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    public static Long getCurrentTenantOrThrow() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new TenantNotDefinedException("Tenant não definido no contexto atual");
        }
        return tenantId;
    }
    
    public static void setCurrentUser(Long userId) {
        log.debug("Definindo usuário atual: {}", userId);
        CURRENT_USER.set(userId);
    }
    
    public static Long getCurrentUser() {
        return CURRENT_USER.get();
    }
    
    public static void clear() {
        log.debug("Limpando contexto do tenant");
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
    
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
