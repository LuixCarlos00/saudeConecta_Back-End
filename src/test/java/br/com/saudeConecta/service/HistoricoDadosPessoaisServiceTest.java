package br.com.saudeConecta.service;

import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.HistoricoDadosPessoaisRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricoDadosPessoaisService - acesso exclusivo do SUPER_ADMIN")
class HistoricoDadosPessoaisServiceTest {

    @Mock private HistoricoDadosPessoaisRepository historicoRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TenantHelper tenantHelper;

    @InjectMocks
    private HistoricoDadosPessoaisService service;

    @Test
    @DisplayName("Usuário sem tenant e sem papel SUPER_ADMIN não lê o histórico global")
    void negaSemTenantNaoRoot() {
        when(tenantHelper.isSuperAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.buscarHistoricoRecentes())
                .isInstanceOf(TenantAccessDeniedException.class);
        assertThatThrownBy(() -> service.buscarHistoricoByUsuario(1L))
                .isInstanceOf(TenantAccessDeniedException.class);
        assertThatThrownBy(() -> service.buscarHistoricoByEntidade("PROFISSIONAL", 1L))
                .isInstanceOf(TenantAccessDeniedException.class);
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("SUPER_ADMIN lê o histórico global")
    void permiteSuperAdmin() {
        when(tenantHelper.isSuperAdmin()).thenReturn(true);
        when(historicoRepository.findTop10ByOrderByCriadoEmDesc()).thenReturn(List.of());

        service.buscarHistoricoRecentes();

        verify(historicoRepository).findTop10ByOrderByCriadoEmDesc();
        verify(historicoRepository, never()).findByUsuario_IdOrderByCriadoEmDesc(any());
    }
}
