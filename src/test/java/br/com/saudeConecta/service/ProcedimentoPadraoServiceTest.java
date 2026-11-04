package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.prontuario.ProcedimentoPadrao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.ProcedimentoPadraoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.presentation.dto.prontuario.ProcedimentoPadraoRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcedimentoPadraoService - isolamento por tenant")
class ProcedimentoPadraoServiceTest {

    private static final Long ORG = 1L;

    @Mock private ProcedimentoPadraoRepository procedimentoRepository;
    @Mock private ProfissionalRepository profissionalRepository;

    @InjectMocks
    private ProcedimentoPadraoService service;

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
        lenient().when(procedimentoRepository.findById(9L))
                .thenReturn(Optional.of(ProcedimentoPadrao.builder().id(9L).ativo(true).build()));
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("toggle, PUT e DELETE de procedimento de outra clínica respondem não encontrado e não gravam")
    void alteracoesOutraClinica() {
        ProcedimentoPadraoRequest request = new ProcedimentoPadraoRequest();
        request.setNomeProcedimento("Limpeza");

        assertThatThrownBy(() -> service.toggleAtivo(9L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizar(9L, request)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.desativar(9L)).isInstanceOf(IllegalArgumentException.class);

        verify(procedimentoRepository, times(3)).findByIdAndOrganizacao_Id(9L, ORG);
        verify(procedimentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar recusa profissional de outra clínica")
    void criarProfissionalOutraClinica() {
        lenient().when(profissionalRepository.findById(3L)).thenReturn(Optional.of(new Profissional()));
        ProcedimentoPadraoRequest request = new ProcedimentoPadraoRequest();
        request.setNomeProcedimento("Limpeza");

        assertThatThrownBy(() -> service.criar(3L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profissional nao encontrado");
        verify(procedimentoRepository, never()).save(any());
    }
}
