package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("PlanejamentoTerapeuticoService - isolamento por tenant e link público")
class PlanejamentoTerapeuticoServiceTest {

    private static final Long ORG = 1L;

    @Mock private PlanejamentoTerapeuticoRepository planejamentoRepository;
    @Spy private TenantHelper tenantHelper = new TenantHelper();

    @InjectMocks
    private PlanejamentoTerapeuticoService service;

    @BeforeEach
    void definirTenant() {
        TenantContext.setCurrentTenant(ORG);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("remover recusa planejamento de outra clínica e não apaga")
    void removerOutraClinica() {
        lenient().when(planejamentoRepository.existsById(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.remover(9L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(planejamentoRepository, never()).deleteById(any());
        verify(planejamentoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("gerarLinkAssinatura recusa prontuário de outra clínica")
    void gerarLinkOutraClinica() {
        lenient().when(planejamentoRepository.findByProntuarioId(eq(5L), any()))
                .thenAnswer(inv -> ORG.equals(inv.getArgument(1)) ? List.of() : List.of(new PlanejamentoTerapeutico()));

        assertThatThrownBy(() -> service.gerarLinkAssinatura(5L, "DENTISTA"))
                .isInstanceOf(IllegalStateException.class);
        verify(planejamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("gerarLinkAssinatura define expiração de 7 dias e não loga o token inteiro")
    void gerarLinkDefineExpiracao(CapturedOutput output) {
        PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder().statusAssinatura("PENDENTE").build();
        when(planejamentoRepository.findByProntuarioId(5L, ORG)).thenReturn(List.of(p));

        String token = service.gerarLinkAssinatura(5L, "DENTISTA");

        assertThat(p.getTokenAssinatura()).isEqualTo(token);
        assertThat(p.getTokenExpiracao())
                .isAfter(LocalDateTime.now().plusDays(6))
                .isBefore(LocalDateTime.now().plusDays(7).plusMinutes(1));
        assertThat(output.getAll()).contains("token=" + token.substring(0, 6))
                .doesNotContain(token);
    }

    @Test
    @DisplayName("buscarPorToken recusa link expirado")
    void buscarPorTokenExpirado() {
        TenantContext.clear();
        PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder()
                .statusAssinatura("PENDENTE")
                .tokenExpiracao(LocalDateTime.now().minusMinutes(1))
                .build();
        when(planejamentoRepository.findByTokenAssinatura("tok")).thenReturn(List.of(p));

        assertThatThrownBy(() -> service.buscarPorToken("tok"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    @DisplayName("buscarPorToken de planejamento já ASSINADO responde os dados mesmo após a expiração")
    void buscarPorTokenAssinadoExpirado() {
        TenantContext.clear();
        PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder()
                .statusAssinatura("ASSINADO")
                .tokenExpiracao(LocalDateTime.now().minusDays(30))
                .build();
        when(planejamentoRepository.findByTokenAssinatura("tok")).thenReturn(List.of(p));

        assertThat(service.buscarPorToken("tok")).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorToken aceita token legado sem expiração (coluna nula)")
    void buscarPorTokenLegado() {
        TenantContext.clear();
        PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder().statusAssinatura("PENDENTE").build();
        when(planejamentoRepository.findByTokenAssinatura("tok")).thenReturn(List.of(p));

        assertThat(service.buscarPorToken("tok")).hasSize(1);
    }

    @Test
    @DisplayName("assinarPorToken repassa o instante atual para a query condicional e não loga o token")
    void assinarPorTokenCondicional(CapturedOutput output) {
        TenantContext.clear();
        when(planejamentoRepository.assinarPorToken(eq("abcdef-1234-5678"), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.assinarPorToken("abcdef-1234-5678", "base64", "10.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(planejamentoRepository).assinarPorToken(eq("abcdef-1234-5678"), eq("base64"), eq("10.0.0.1"),
                any(LocalDateTime.class));
        assertThat(output.getAll()).contains("token=abcdef***").doesNotContain("abcdef-1234-5678");
    }
}
