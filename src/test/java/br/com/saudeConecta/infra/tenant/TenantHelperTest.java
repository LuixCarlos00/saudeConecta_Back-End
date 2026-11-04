package br.com.saudeConecta.infra.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantHelper - checagem explícita de SUPER_ADMIN")
class TenantHelperTest {

    private final TenantHelper tenantHelper = new TenantHelper();

    @AfterEach
    void limpar() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void autenticarCom(String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario", null, authorities));
    }

    @Test
    @DisplayName("isSuperAdmin é true somente com ROLE_SUPER_ADMIN")
    void isSuperAdminSomenteComRole() {
        autenticarCom("ROLE_ADMIN", "ROLE_PROFISSIONAL", "ROLE_RECEPCIONISTA");
        assertThat(tenantHelper.isSuperAdmin()).isFalse();

        autenticarCom("ROLE_SUPER_ADMIN", "ROLE_ADMIN");
        assertThat(tenantHelper.isSuperAdmin()).isTrue();
    }

    @Test
    @DisplayName("isSuperAdmin é false sem autenticação")
    void isSuperAdminFalseSemAutenticacao() {
        assertThat(tenantHelper.isSuperAdmin()).isFalse();
    }

    @Test
    @DisplayName("Retorna o tenant do token quando existe")
    void retornaTenantQuandoExiste() {
        TenantContext.setCurrentTenant(5L);
        autenticarCom("ROLE_ADMIN");

        assertThat(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Retorna null (visão global) apenas para SUPER_ADMIN sem tenant")
    void retornaNullParaSuperAdminSemTenant() {
        autenticarCom("ROLE_SUPER_ADMIN");

        assertThat(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).isNull();
    }

    @Test
    @DisplayName("Nega acesso quando não há tenant e o usuário não é SUPER_ADMIN (falha fechada)")
    void negaSemTenantENaoSuperAdmin() {
        autenticarCom("ROLE_ADMIN", "ROLE_PROFISSIONAL", "ROLE_RECEPCIONISTA");

        assertThatThrownBy(() -> tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin())
                .isInstanceOf(TenantNotDefinedException.class);
    }
}
