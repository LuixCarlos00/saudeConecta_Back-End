package br.com.saudeConecta.service;

import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PlanejamentoTerapeuticoRepository;
import br.com.saudeConecta.presentation.dto.dashboard.SaldoFinanceiroResponse;
import br.com.saudeConecta.presentation.dto.dashboard.SaldoFinanceiroResponse.SaldoPorPeriodo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Serviço que calcula o saldo financeiro do dashboard,
 * combinando valores de consultas realizadas + procedimentos terapêuticos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaldoFinanceiroService {

    private final ConsultaRepository consultaRepository;
    private final PlanejamentoTerapeuticoRepository planejamentoRepository;

    private static final Locale PT_BR = new Locale("pt", "BR");

    /**
     * Calcula o saldo financeiro para uma organização em um período.
     *
     * @param organizacaoId ID da organização
     * @param inicio        data de início
     * @param fim           data de fim
     * @param agruparPor    "mes" ou "semana"
     * @return estatísticas financeiras com detalhamento por sub-período
     */
    @Transactional(readOnly = true)
    public SaldoFinanceiroResponse calcularSaldo(Long organizacaoId, LocalDate inicio, LocalDate fim, String agruparPor) {
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime fimDateTime = fim.atTime(LocalTime.MAX);

        // Totais gerais
        List<Object[]> totalConsultas = consultaRepository
                .somarValorConsultasRealizadasPorOrganizacao(organizacaoId, inicioDateTime, fimDateTime);
        List<Object[]> totalProcs = planejamentoRepository
                .somarValorProcedimentosPorOrganizacao(organizacaoId, inicio, fim);

        BigDecimal somaConsultas = extrairBigDecimal(totalConsultas, 0);
        long qtdConsultas = extrairLong(totalConsultas, 1);
        BigDecimal somaProcs = extrairBigDecimal(totalProcs, 0);
        long qtdProcs = extrairLong(totalProcs, 1);

        // Detalhamento por período
        List<SaldoPorPeriodo> detalhamento;
        if ("semana".equalsIgnoreCase(agruparPor)) {
            detalhamento = calcularDetalhamentoPorSemana(organizacaoId, inicio, fim, inicioDateTime, fimDateTime);
        } else {
            detalhamento = calcularDetalhamentoPorMes(organizacaoId, inicio, fim, inicioDateTime, fimDateTime);
        }

        return new SaldoFinanceiroResponse(
                somaConsultas,
                somaProcs,
                somaConsultas.add(somaProcs),
                qtdConsultas,
                qtdProcs,
                detalhamento
        );
    }

    private List<SaldoPorPeriodo> calcularDetalhamentoPorMes(
            Long orgId, LocalDate inicio, LocalDate fim,
            LocalDateTime inicioDateTime, LocalDateTime fimDateTime) {

        List<Object[]> consultasPorMes = consultaRepository
                .somarValorConsultasAgrupadasPorMes(orgId, inicioDateTime, fimDateTime);
        List<Object[]> procsPorMes = planejamentoRepository
                .somarValorProcedimentosAgrupadosPorMes(orgId, inicio, fim);

        // Chave = "AAAA-MM"
        Map<String, BigDecimal> consultasMap = new LinkedHashMap<>();
        Map<String, BigDecimal> procsMap = new LinkedHashMap<>();

        for (Object[] row : consultasPorMes) {
            String chave = String.format("%04d-%02d", toInt(row[0]), toInt(row[1]));
            consultasMap.put(chave, toBigDecimal(row[2]));
        }
        for (Object[] row : procsPorMes) {
            String chave = String.format("%04d-%02d", toInt(row[0]), toInt(row[1]));
            procsMap.put(chave, toBigDecimal(row[2]));
        }

        // Gerar lista ordenada com todos os meses no intervalo
        Set<String> todasChaves = new TreeSet<>();
        todasChaves.addAll(consultasMap.keySet());
        todasChaves.addAll(procsMap.keySet());

        // Se vazio, gerar chaves para cada mês do período
        if (todasChaves.isEmpty()) {
            LocalDate cursor = inicio.withDayOfMonth(1);
            while (!cursor.isAfter(fim)) {
                todasChaves.add(String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue()));
                cursor = cursor.plusMonths(1);
            }
        }

        List<SaldoPorPeriodo> resultado = new ArrayList<>();
        for (String chave : todasChaves) {
            BigDecimal vc = consultasMap.getOrDefault(chave, BigDecimal.ZERO);
            BigDecimal vp = procsMap.getOrDefault(chave, BigDecimal.ZERO);
            String[] partes = chave.split("-");
            int mes = Integer.parseInt(partes[1]);
            int ano = Integer.parseInt(partes[0]);
            String label = LocalDate.of(ano, mes, 1)
                    .getMonth().getDisplayName(TextStyle.SHORT, PT_BR) + "/" + ano;
            resultado.add(new SaldoPorPeriodo(label, vc, vp, vc.add(vp)));
        }
        return resultado;
    }

    private List<SaldoPorPeriodo> calcularDetalhamentoPorSemana(
            Long orgId, LocalDate inicio, LocalDate fim,
            LocalDateTime inicioDateTime, LocalDateTime fimDateTime) {

        List<Object[]> consultasPorSemana = consultaRepository
                .somarValorConsultasAgrupadasPorSemana(orgId, inicioDateTime, fimDateTime);
        List<Object[]> procsPorSemana = planejamentoRepository
                .somarValorProcedimentosAgrupadosPorSemana(orgId, inicio, fim);

        Map<String, BigDecimal> consultasMap = new LinkedHashMap<>();
        Map<String, BigDecimal> procsMap = new LinkedHashMap<>();

        for (Object[] row : consultasPorSemana) {
            String chave = String.format("%04d-S%02d", toInt(row[0]), toInt(row[1]));
            consultasMap.put(chave, toBigDecimal(row[2]));
        }
        for (Object[] row : procsPorSemana) {
            String chave = String.format("%04d-S%02d", toInt(row[0]), toInt(row[1]));
            procsMap.put(chave, toBigDecimal(row[2]));
        }

        Set<String> todasChaves = new TreeSet<>();
        todasChaves.addAll(consultasMap.keySet());
        todasChaves.addAll(procsMap.keySet());

        List<SaldoPorPeriodo> resultado = new ArrayList<>();
        for (String chave : todasChaves) {
            BigDecimal vc = consultasMap.getOrDefault(chave, BigDecimal.ZERO);
            BigDecimal vp = procsMap.getOrDefault(chave, BigDecimal.ZERO);
            String label = "Sem " + chave.split("-S")[1];
            resultado.add(new SaldoPorPeriodo(label, vc, vp, vc.add(vp)));
        }
        return resultado;
    }

    // ── Helpers de conversão ─────────────────────────────────────────────────

    private BigDecimal extrairBigDecimal(List<Object[]> rows, int index) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null) return BigDecimal.ZERO;
        Object val = rows.get(0)[index];
        return val instanceof BigDecimal bd ? bd : new BigDecimal(val.toString());
    }

    private long extrairLong(List<Object[]> rows, int index) {
        if (rows == null || rows.isEmpty() || rows.get(0) == null) return 0;
        Object val = rows.get(0)[index];
        return val instanceof Number n ? n.longValue() : 0;
    }

    private int toInt(Object val) {
        return val instanceof Number n ? n.intValue() : 0;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        return val instanceof BigDecimal bd ? bd : new BigDecimal(val.toString());
    }
}
