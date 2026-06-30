package br.com.saudeConecta.service;

import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanejamentoTerapeuticoRepository;
import br.com.saudeConecta.presentation.dto.dashboard.SaldoFinanceiroResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaldoFinanceiroService - Testes unitários")
class SaldoFinanceiroServiceTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private PlanejamentoTerapeuticoRepository planejamentoRepository;

    @InjectMocks
    private SaldoFinanceiroService service;

    private final LocalDate INICIO = LocalDate.of(2024, 1, 1);
    private final LocalDate FIM = LocalDate.of(2024, 3, 31);

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Object[]... arrays) {
        return (List<Object[]>) (List<?>) Arrays.asList(arrays);
    }

    // ========== calcularSaldo por mês ==========

    @Nested
    @DisplayName("calcularSaldo agrupado por mês")
    class CalcularSaldoPorMes {

        @Test
        @DisplayName("Deve calcular saldo corretamente com dados de consultas e procedimentos")
        void deveCalcularSaldoComDados() {
            List<Object[]> totalConsultas = rows(new Object[]{BigDecimal.valueOf(1500), 10L});
            List<Object[]> totalProcs = rows(new Object[]{BigDecimal.valueOf(500), 5L});
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(totalConsultas);
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(totalProcs);
            List<Object[]> mesConsultas = Collections.emptyList();
            List<Object[]> mesProcs = Collections.emptyList();
            when(consultaRepository.somarValorConsultasAgrupadasPorMes(eq(1L), any(), any()))
                    .thenReturn(mesConsultas);
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorMes(eq(1L), any(), any()))
                    .thenReturn(mesProcs);

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "mes");

            assertThat(response).isNotNull();
            assertThat(response.totalConsultas()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(response.totalProcedimentos()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(response.totalGeral()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            assertThat(response.quantidadeConsultas()).isEqualTo(10L);
            assertThat(response.quantidadeProcedimentos()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Deve retornar totais zero quando não há dados")
        void deveRetornarZerosQuandoSemDados() {
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(consultaRepository.somarValorConsultasAgrupadasPorMes(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorMes(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "mes");

            assertThat(response.totalConsultas()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.totalProcedimentos()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.totalGeral()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Deve gerar detalhamento por mês com dados agrupados")
        void deveGerarDetalhamentoPorMes() {
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            List<Object[]> cMes = rows(new Object[]{2024, 1, BigDecimal.valueOf(1000)});
            List<Object[]> pMes = rows(new Object[]{2024, 1, BigDecimal.valueOf(200)});
            when(consultaRepository.somarValorConsultasAgrupadasPorMes(eq(1L), any(), any()))
                    .thenReturn(cMes);
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorMes(eq(1L), any(), any()))
                    .thenReturn(pMes);

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "mes");

            assertThat(response.detalhamento()).isNotEmpty();
            assertThat(response.detalhamento().get(0).valorTotal())
                    .isEqualByComparingTo(BigDecimal.valueOf(1200));
        }

        @Test
        @DisplayName("Deve gerar chaves para cada mês do período quando sem dados")
        void deveGerarChavesMesesVazios() {
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(consultaRepository.somarValorConsultasAgrupadasPorMes(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorMes(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "mes");

            assertThat(response.detalhamento()).hasSize(3);
        }
    }

    // ========== calcularSaldo por semana ==========

    @Nested
    @DisplayName("calcularSaldo agrupado por semana")
    class CalcularSaldoPorSemana {

        @Test
        @DisplayName("Deve calcular saldo agrupado por semana")
        void deveCalcularPorSemana() {
            List<Object[]> totSem = rows(new Object[]{BigDecimal.valueOf(800), 4L});
            List<Object[]> totProcVazio = Collections.emptyList();
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(totSem);
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(totProcVazio);
            List<Object[]> sSem = rows(new Object[]{2024, 1, BigDecimal.valueOf(400)});
            List<Object[]> pSemVazio = Collections.emptyList();
            when(consultaRepository.somarValorConsultasAgrupadasPorSemana(eq(1L), any(), any()))
                    .thenReturn(sSem);
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorSemana(eq(1L), any(), any()))
                    .thenReturn(pSemVazio);

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "semana");

            assertThat(response).isNotNull();
            assertThat(response.totalConsultas()).isEqualByComparingTo(BigDecimal.valueOf(800));
            assertThat(response.detalhamento()).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar detalhamento vazio quando sem dados por semana")
        void deveRetornarDetalhamentoVazioSemDados() {
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(consultaRepository.somarValorConsultasAgrupadasPorSemana(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorSemana(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "semana");

            assertThat(response.detalhamento()).isEmpty();
        }

        @Test
        @DisplayName("Deve consolidar consultas e procedimentos por semana")
        void deveConsolidarPorSemana() {
            when(consultaRepository.somarValorConsultasRealizadasPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(planejamentoRepository.somarValorProcedimentosPorOrganizacao(eq(1L), any(), any()))
                    .thenReturn(Collections.emptyList());
            List<Object[]> cSem2 = rows(new Object[]{2024, 1, BigDecimal.valueOf(200)}, new Object[]{2024, 2, BigDecimal.valueOf(300)});
            List<Object[]> pSem2 = rows(new Object[]{2024, 2, BigDecimal.valueOf(100)});
            when(consultaRepository.somarValorConsultasAgrupadasPorSemana(eq(1L), any(), any()))
                    .thenReturn(cSem2);
            when(planejamentoRepository.somarValorProcedimentosAgrupadosPorSemana(eq(1L), any(), any()))
                    .thenReturn(pSem2);

            SaldoFinanceiroResponse response = service.calcularSaldo(1L, INICIO, FIM, "semana");

            assertThat(response.detalhamento()).hasSize(2);
        }
    }
}
