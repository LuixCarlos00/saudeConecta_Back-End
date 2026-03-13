package br.com.saudeConecta.presentation.dto.consulta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com todas as estatísticas necessárias para o dashboard do AdminOrg.
 * Calculado a partir de uma única consulta ao banco, filtrando pela semana atual (segunda a domingo).
 *
 * Campos derivados no backend a partir dos dados brutos da semana:
 * - consultasHoje: total de consultas agendadas para o dia atual
 * - consultasAguardando: consultas com status AGENDADA no dia atual
 * - consultasAtendidas: consultas com status REALIZADA no dia atual
 * - consultasSemana: total de consultas da semana (todos os status)
 * - canceladosSemana: consultas com status CANCELADA na semana
 * - confirmadosSemana: consultas com status CONFIRMADA na semana
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstatisticasDashboardAdminOrgResponse {

    /** Total de consultas (qualquer status) agendadas para hoje */
    private Long consultasHoje;

    /** Consultas com status AGENDADA para hoje (aguardando atendimento) */
    private Long consultasAguardando;

    /** Consultas com status REALIZADA para hoje (já atendidas) */
    private Long consultasAtendidas;

    /** Total de consultas da semana (segunda a domingo), qualquer status */
    private Long consultasSemana;

    /** Consultas com status CANCELADA na semana atual */
    private Long canceladosSemana;

    /** Consultas com status CONFIRMADA na semana atual */
    private Long confirmadosSemana;
}
