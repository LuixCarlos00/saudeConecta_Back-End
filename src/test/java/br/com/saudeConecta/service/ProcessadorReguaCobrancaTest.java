package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import br.com.saudeConecta.domain.planos.OrigemCancelamento;
import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.infra.exceptions.BusinessException;
import br.com.saudeConecta.infrastructure.persistence.repository.AssinaturaTenantRepository;
import br.com.saudeConecta.service.mercadopago.AssinaturaMercadoPagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessadorReguaCobranca - Testes unitários")
class ProcessadorReguaCobrancaTest {

    private static final Long ID = 42L;

    @Mock private AssinaturaTenantRepository assinaturaTenantRepository;
    @Mock private AssinaturaTenantService assinaturaTenantService;
    @Mock private AssinaturaMercadoPagoService assinaturaMercadoPagoService;

    @InjectMocks
    private ProcessadorReguaCobranca processador;

    private AssinaturaTenant assinatura;

    @BeforeEach
    void setUp() {
        assinatura = AssinaturaTenant.builder().id(ID).status(StatusAssinatura.INADIMPLENTE).build();
    }

    @Test
    @DisplayName("Marcar inadimplente delega ao serviço de assinatura")
    void marcarInadimplente() {
        processador.marcarInadimplente(ID);

        verify(assinaturaTenantService).marcarInadimplente(ID);
    }

    @Test
    @DisplayName("Suspender suspende localmente e pausa no MP a entidade recarregada na mesma transação")
    void suspender() {
        when(assinaturaTenantRepository.findById(ID)).thenReturn(Optional.of(assinatura));

        processador.suspender(ID);

        var ordem = inOrder(assinaturaTenantService, assinaturaMercadoPagoService);
        ordem.verify(assinaturaTenantService).suspender(ID);
        ordem.verify(assinaturaMercadoPagoService).pausar(assinatura);
    }

    @Test
    @DisplayName("Cancelar recarrega a assinatura e cancela com origem SISTEMA")
    void cancelar() {
        when(assinaturaTenantRepository.findById(ID)).thenReturn(Optional.of(assinatura));

        processador.cancelar(ID);

        verify(assinaturaMercadoPagoService).cancelar(assinatura, OrigemCancelamento.SISTEMA);
    }

    @Test
    @DisplayName("Sincronizar recarrega a assinatura e devolve se houve divergência")
    void sincronizar() {
        when(assinaturaTenantRepository.findById(ID)).thenReturn(Optional.of(assinatura));
        when(assinaturaMercadoPagoService.sincronizar(assinatura)).thenReturn(true);

        assertThat(processador.sincronizar(ID)).isTrue();
    }

    @Test
    @DisplayName("Assinatura que sumiu entre a busca e o processamento gera BusinessException")
    void assinaturaInexistente() {
        when(assinaturaTenantRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processador.cancelar(ID)).isInstanceOf(BusinessException.class);
    }
}
