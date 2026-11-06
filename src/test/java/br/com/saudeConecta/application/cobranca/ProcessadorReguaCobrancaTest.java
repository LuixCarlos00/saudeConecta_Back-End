package br.com.saudeConecta.application.cobranca;

import br.com.saudeConecta.application.assinatura.AssinaturaMercadoPagoService;
import br.com.saudeConecta.application.assinatura.AssinaturaTenantService;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProcessadorReguaCobranca - Testes unitários")
class ProcessadorReguaCobrancaTest {

    private static final Long ID = 42L;
    private static final LocalDate LIMITE = LocalDate.now().minusDays(5);
    private static final LocalDateTime LIMITE_CANCELAMENTO = LocalDate.now().minusDays(30).atStartOfDay();

    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private AssinaturaTenantService assinaturaTenantService;
    @Mock private AssinaturaMercadoPagoService assinaturaMercadoPagoService;

    @InjectMocks
    private ProcessadorReguaCobranca processador;

    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        assinatura = AssinaturaTenant.builder()
                .id(ID)
                .status(StatusAssinatura.INADIMPLENTE)
                .dataVencimento(LIMITE.minusDays(20))
                .build();
        when(assinaturaTenantRepository.findByIdParaAtualizacao(ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaTenantRepository.findById(ID)).thenReturn(Optional.of(assinatura));
    }

    @Test
    @DisplayName("Marcar inadimplente relê com lock e delega quando a condição ainda vale")
    void marcarInadimplente() {
        assinatura.setStatus(StatusAssinatura.ATIVA);

        assertThat(processador.marcarInadimplente(ID, LIMITE)).isTrue();

        verify(assinaturaTenantRepository).findByIdParaAtualizacao(ID);
        verify(assinaturaTenantService).marcarInadimplente(ID);
    }

    @Test
    @DisplayName("H2 - Marcar inadimplente ignora assinatura renovada depois da leitura do lote")
    void marcarInadimplenteIgnoraRenovada() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(LocalDate.now().plusMonths(1));

        assertThat(processador.marcarInadimplente(ID, LIMITE)).isFalse();

        verify(assinaturaTenantService, never()).marcarInadimplente(anyLong());
    }

    @Test
    @DisplayName("H2 - Marcar inadimplente ignora assinatura que virou cortesia")
    void marcarInadimplenteIgnoraCortesia() {
        assinatura.setStatus(StatusAssinatura.TRIAL);
        assinatura.setCortesia(true);

        assertThat(processador.marcarInadimplente(ID, LIMITE)).isFalse();

        verify(assinaturaTenantService, never()).marcarInadimplente(anyLong());
    }

    @Test
    @DisplayName("Suspender suspende localmente e pausa no MP a entidade relida com lock")
    void suspender() {
        assertThat(processador.suspender(ID, LIMITE)).isTrue();

        var ordem = inOrder(assinaturaTenantRepository, assinaturaTenantService, assinaturaMercadoPagoService);
        ordem.verify(assinaturaTenantRepository).findByIdParaAtualizacao(ID);
        ordem.verify(assinaturaTenantService).suspender(ID);
        ordem.verify(assinaturaMercadoPagoService).pausar(assinatura);
    }

    @Test
    @DisplayName("H2 - Suspender ignora assinatura que voltou para ATIVA entre o lote e o processamento")
    void suspenderIgnoraAtiva() {
        assinatura.setStatus(StatusAssinatura.ATIVA);

        assertThat(processador.suspender(ID, LIMITE)).isFalse();

        verifyNoInteractions(assinaturaTenantService, assinaturaMercadoPagoService);
    }

    @Test
    @DisplayName("H2 - Suspender ignora inadimplente cujo vencimento já não é anterior ao limite")
    void suspenderIgnoraVencimentoNovo() {
        assinatura.setDataVencimento(LIMITE);

        assertThat(processador.suspender(ID, LIMITE)).isFalse();

        verifyNoInteractions(assinaturaTenantService, assinaturaMercadoPagoService);
    }

    @Test
    @DisplayName("Cancelar relê com lock e cancela com origem SISTEMA")
    void cancelar() {
        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setDataSuspensao(LIMITE_CANCELAMENTO.minusDays(1));

        assertThat(processador.cancelar(ID, LIMITE_CANCELAMENTO)).isTrue();

        verify(assinaturaMercadoPagoService).cancelar(assinatura, OrigemCancelamento.SISTEMA);
    }

    @Test
    @DisplayName("H2 - Cancelar ignora suspensa que foi reativada ou suspensa há menos tempo")
    void cancelarIgnoraForaDaCondicao() {
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataSuspensao(LIMITE_CANCELAMENTO.minusDays(1));
        assertThat(processador.cancelar(ID, LIMITE_CANCELAMENTO)).isFalse();

        assinatura.setStatus(StatusAssinatura.SUSPENSA);
        assinatura.setDataSuspensao(LIMITE_CANCELAMENTO.plusDays(1));
        assertThat(processador.cancelar(ID, LIMITE_CANCELAMENTO)).isFalse();

        verify(assinaturaMercadoPagoService, never()).cancelar(any(), any());
    }

    @Test
    @DisplayName("Sincronizar relê com lock e devolve se houve divergência")
    void sincronizar() {
        assinatura.setMpPreapprovalId("pa-1");
        when(assinaturaMercadoPagoService.sincronizar(assinatura)).thenReturn(true);

        assertThat(processador.sincronizar(ID)).isTrue();
        verify(assinaturaTenantRepository).findByIdParaAtualizacao(ID);
    }

    @Test
    @DisplayName("H2 - Sincronizar ignora assinatura cancelada depois da leitura do lote")
    void sincronizarIgnoraCancelada() {
        assinatura.setMpPreapprovalId("pa-1");
        assinatura.setStatus(StatusAssinatura.CANCELADA);

        assertThat(processador.sincronizar(ID)).isFalse();
        verify(assinaturaMercadoPagoService, never()).sincronizar(any());
    }

    @Test
    @DisplayName("Assinatura que sumiu entre a busca e o processamento gera BusinessException")
    void assinaturaInexistente() {
        when(assinaturaTenantRepository.findByIdParaAtualizacao(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processador.cancelar(ID, LIMITE_CANCELAMENTO)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("R3 - Limpeza de cortesia: relê com lock e só desvincula cortesia que ainda tem preapproval")
    void limparPreapprovalDeCortesia() {
        assinatura.setCortesia(true);
        assinatura.setMpPreapprovalId("pa-1");

        assertThat(processador.limparPreapprovalDeCortesia(ID)).isTrue();
        verify(assinaturaTenantRepository).findByIdParaAtualizacao(ID);
        verify(assinaturaMercadoPagoService).limparPreapprovalDeCortesia(assinatura);

        // Deixou de ser cortesia (ou já não tem preapproval) entre a leitura do lote e o lock
        reset(assinaturaMercadoPagoService);
        assinatura.setCortesia(false);
        assertThat(processador.limparPreapprovalDeCortesia(ID)).isFalse();
        assinatura.setCortesia(true);
        assinatura.setMpPreapprovalId(null);
        assertThat(processador.limparPreapprovalDeCortesia(ID)).isFalse();
        verifyNoInteractions(assinaturaMercadoPagoService);
    }
}
