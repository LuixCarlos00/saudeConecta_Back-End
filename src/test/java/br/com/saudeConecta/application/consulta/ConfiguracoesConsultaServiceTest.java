package br.com.saudeConecta.application.consulta;

import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ConfiguracoesConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfiguracoesConsultaService - isolamento por tenant")
class ConfiguracoesConsultaServiceTest {

    private static final Long ORG = 1L;

    @Mock private ConfiguracoesConsultaRepository configuracoesConsultaRepository;
    @Mock private OrganizacaoRepository organizacaoRepository;
    @Spy private TenantHelper tenantHelper = new TenantHelper();

    @InjectMocks
    private ConfiguracoesConsultaService service;

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /organizacao/{outraOrg}: nega leitura da configuração de outra clínica")
    void buscarOutraClinica() {
        assertThatThrownBy(() -> service.buscarConfiguracaoPorOrganizacao(2L))
                .isInstanceOf(TenantAccessDeniedException.class);
        verifyNoInteractions(configuracoesConsultaRepository);
    }

    @Test
    @DisplayName("POST /organizacao/{outraOrg}: nega criar configuração para outra clínica")
    void criarOutraClinica() {
        assertThatThrownBy(() -> service.criarConfiguracao(2L, true, "x"))
                .isInstanceOf(TenantAccessDeniedException.class);
        verify(configuracoesConsultaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Configuração atual da própria clínica continua acessível")
    void buscarAtualPropria() {
        when(configuracoesConsultaRepository.findByOrganizacaoId(ORG)).thenReturn(Optional.empty());

        assertThat(service.buscarConfiguracaoAtual().getPularParaConfirmado()).isFalse();
    }
}
