package br.com.saudeConecta.application.suporte;

import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.application.suporte.dto.AtualizarStatusChamadoRequest;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.config.tenant.TenantNotDefinedException;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.suporte.ChamadoSuporte;
import br.com.saudeConecta.domain.suporte.StatusChamado;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChamadoSuporteService - isolamento por tenant")
class ChamadoSuporteServiceTest {

    @Mock private ChamadoSuporteRepository chamadoSuporteRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private SecretariaRepository secretariaRepository;
    @Mock private AdminOrganizacaoRepository adminOrganizacaoRepository;
    @Mock private EmailNotificacaoService emailNotificacaoService;
    @Mock private TenantHelper tenantHelper;

    @InjectMocks
    private ChamadoSuporteService service;

    private ChamadoSuporte chamadoDaOrg(Long orgId) {
        Organizacao org = new Organizacao();
        org.setId(orgId);
        ChamadoSuporte c = new ChamadoSuporte();
        c.setId("ABC123");
        c.setOrganizacao(org);
        c.setStatus(StatusChamado.EM_ANALISE);
        return c;
    }

    @Test
    @DisplayName("listarPorTenant nega sem tenant quando não é SUPER_ADMIN")
    void listarNegaSemTenantNaoRoot() {
        when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin())
                .thenThrow(new TenantNotDefinedException("Tenant não definido"));

        var pagina = PageRequest.of(0, 10);
        assertThatThrownBy(() -> service.listarPorTenant(null, null, pagina))
                .isInstanceOf(TenantNotDefinedException.class);
        verify(chamadoSuporteRepository, never()).findAllWithFilters(any(), any(), any());
    }

    @Test
    @DisplayName("listarPorTenant devolve a visão global ao SUPER_ADMIN")
    void listarGlobalParaRoot() {
        when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(null);
        when(chamadoSuporteRepository.findAllWithFilters(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listarPorTenant(null, null, PageRequest.of(0, 10));

        verify(chamadoSuporteRepository).findAllWithFilters(any(), any(), any());
    }

    @Test
    @DisplayName("buscarPorId nega sem tenant quando não é SUPER_ADMIN")
    void buscarNegaSemTenantNaoRoot() {
        when(chamadoSuporteRepository.findByIdWithAnexos("ABC123")).thenReturn(Optional.of(chamadoDaOrg(2L)));
        when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin())
                .thenThrow(new TenantNotDefinedException("Tenant não definido"));

        assertThatThrownBy(() -> service.buscarPorId("ABC123"))
                .isInstanceOf(TenantNotDefinedException.class);
    }

    @Test
    @DisplayName("buscarPorId de chamado de outra clínica responde como não encontrado")
    void buscarOutraClinica() {
        when(chamadoSuporteRepository.findByIdWithAnexos("ABC123")).thenReturn(Optional.of(chamadoDaOrg(2L)));
        when(tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin()).thenReturn(1L);

        assertThatThrownBy(() -> service.buscarPorId("ABC123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("atualizarStatus exige papel SUPER_ADMIN, não apenas ausência de tenant")
    void atualizarStatusExigeRoot() {
        when(tenantHelper.isSuperAdmin()).thenReturn(false);

        var requisicao = new AtualizarStatusChamadoRequest(StatusChamado.EM_ANALISE, null);
        assertThatThrownBy(() -> service.atualizarStatus("ABC123", requisicao))
                .isInstanceOf(TenantAccessDeniedException.class);
        verify(chamadoSuporteRepository, never()).findByIdWithAnexos(any());
    }
}
