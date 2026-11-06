package br.com.saudeConecta.presentation.controller.organizacao;

import br.com.saudeConecta.application.organizacao.AdminOrganizacaoService;
import br.com.saudeConecta.config.web.RateLimiter;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("CadastroPublicoController - chave do rate limit")
class CadastroPublicoControllerTest {

    @Test
    @DisplayName("X-Forwarded-For forjado não muda a chave do rate limit (usa getRemoteAddr)")
    void xffForjadoNaoMudaChave() {
        AdminOrganizacaoService adminService = mock(AdminOrganizacaoService.class);
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.permitir(anyString(), anyInt())).thenReturn(true);
        AdminOrganizacao admin = mock(AdminOrganizacao.class);
        Organizacao org = mock(Organizacao.class);
        when(admin.getOrganizacao()).thenReturn(org);
        when(adminService.cadastrarTenantPublico(any())).thenReturn(admin);
        CadastroPublicoController controller = new CadastroPublicoController(adminService, rateLimiter);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.1.1.1");
        req.addHeader("X-Forwarded-For", "6.6.6.6, 7.7.7.7");

        controller.cadastrar(null, req);

        verify(rateLimiter).permitir(eq("cadastro-saas:10.1.1.1"), anyInt());
    }
}
