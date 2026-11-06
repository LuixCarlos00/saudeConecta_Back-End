package br.com.saudeConecta.config.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantHelper {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    
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
        return possuiPapel(ROLE_SUPER_ADMIN);
    }

    /**
     * Indica se o usuário autenticado é gestor da clínica (ROLE_ADMIN). O ROOT também
     * possui ROLE_ADMIN e passa nesta verificação, como passa no {@code hasRole('ADMIN')}.
     *
     * @return true quando a autenticação atual contém ROLE_ADMIN
     */
    public boolean isAdmin() {
        return possuiPapel(ROLE_ADMIN);
    }

    /**
     * Garante que o usuário logado pode editar o cadastro de outro usuário da própria clínica:
     * só o gestor (ROLE_ADMIN) ou o próprio dono do cadastro. Chamar depois de confirmar que o
     * cadastro pertence à organização do token (cadastro de outra clínica responde 404 antes).
     *
     * @param usuarioDonoId ID do usuário dono do cadastro (null quando o cadastro não tem login)
     * @throws TenantAccessDeniedException quando o logado não é gestor nem o dono
     */
    public void validarGestorOuDono(Long usuarioDonoId) {
        if (isAdmin()) {
            return;
        }
        Long usuarioLogadoId = getCurrentUserId();
        if (usuarioLogadoId == null || !usuarioLogadoId.equals(usuarioDonoId)) {
            throw new TenantAccessDeniedException(
                "Acesso negado: apenas o gestor ou o próprio usuário pode alterar este cadastro"
            );
        }
    }

    private boolean possuiPapel(String papel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> papel.equals(a.getAuthority()));
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
