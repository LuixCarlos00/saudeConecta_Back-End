package br.com.saudeConecta.presentation.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta com estatísticas financeiras do dashboard.
 * Combina valores de consultas realizadas + procedimentos terapêuticos.
 *
 * @param totalConsultas           soma dos valores de consultas realizadas
 * @param totalProcedimentos       soma dos valores de procedimentos terapêuticos
 * @param totalGeral               soma total (consultas + procedimentos)
 * @param quantidadeConsultas      quantidade de consultas realizadas no período
 * @param quantidadeProcedimentos  quantidade de procedimentos no período
 * @param detalhamento             breakdown por sub-período (semana ou mês)
 */
public record SaldoFinanceiroResponse(
        BigDecimal totalConsultas,
        BigDecimal totalProcedimentos,
        BigDecimal totalGeral,
        long quantidadeConsultas,
        long quantidadeProcedimentos,
        List<SaldoPorPeriodo> detalhamento
) {

    /**
     * Detalhamento financeiro agrupado por sub-período.
     *
     * @param periodo              label do período (ex: "Sem 1", "Mar/2026")
     * @param valorConsultas       soma de consultas no sub-período
     * @param valorProcedimentos   soma de procedimentos no sub-período
     * @param valorTotal           soma total no sub-período
     */
    public record SaldoPorPeriodo(
            String periodo,
            BigDecimal valorConsultas,
            BigDecimal valorProcedimentos,
            BigDecimal valorTotal
    ) {}
}
