package br.com.saudeConecta.config.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Test
    @DisplayName("isAdmin é true para GESTOR e ROOT (ambos têm ROLE_ADMIN) e false para CLINICO/ASSISTENTE")
    void isAdminPorPapel() {
        autenticarCom("ROLE_ADMIN", "ROLE_PROFISSIONAL", "ROLE_RECEPCIONISTA");
        assertThat(tenantHelper.isAdmin()).isTrue();

        autenticarCom("ROLE_SUPER_ADMIN", "ROLE_ADMIN");
        assertThat(tenantHelper.isAdmin()).isTrue();

        autenticarCom("ROLE_PROFISSIONAL");
        assertThat(tenantHelper.isAdmin()).isFalse();

        autenticarCom("ROLE_RECEPCIONISTA");
        assertThat(tenantHelper.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("validarGestorOuDono: gestor passa mesmo não sendo o dono")
    void gestorPassaSemSerDono() {
        autenticarCom("ROLE_ADMIN", "ROLE_PROFISSIONAL", "ROLE_RECEPCIONISTA");
        TenantContext.setCurrentUser(1L);

        assertThatCode(() -> tenantHelper.validarGestorOuDono(2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarGestorOuDono: o dono do cadastro passa sem ser gestor")
    void donoPassaSemSerGestor() {
        autenticarCom("ROLE_PROFISSIONAL");
        TenantContext.setCurrentUser(7L);

        assertThatCode(() -> tenantHelper.validarGestorOuDono(7L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarGestorOuDono: CLINICO/ASSISTENTE editando cadastro de outro usuário é negado")
    void naoGestorEditandoOutroEhNegado() {
        autenticarCom("ROLE_RECEPCIONISTA");
        TenantContext.setCurrentUser(7L);

        assertThatThrownBy(() -> tenantHelper.validarGestorOuDono(8L))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    @DisplayName("validarGestorOuDono: cadastro sem login ou requisição sem usuário é negado para quem não é gestor")
    void semDonoOuSemUsuarioLogadoEhNegado() {
        autenticarCom("ROLE_PROFISSIONAL");

        assertThatThrownBy(() -> tenantHelper.validarGestorOuDono(7L))
                .isInstanceOf(TenantAccessDeniedException.class);

        TenantContext.setCurrentUser(7L);
        assertThatThrownBy(() -> tenantHelper.validarGestorOuDono(null))
                .isInstanceOf(TenantAccessDeniedException.class);
    }
}
