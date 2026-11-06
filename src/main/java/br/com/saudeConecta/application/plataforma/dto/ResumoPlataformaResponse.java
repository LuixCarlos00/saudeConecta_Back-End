package br.com.saudeConecta.application.plataforma.dto;

import java.math.BigDecimal;

/**
 * Números dos cards do dashboard da plataforma.
 *
 * @param clinicasTotal       clínicas com assinatura vigente, em qualquer status
 * @param clinicasAtivas      TRIAL, ATIVA ou INADIMPLENTE (cortesia conta)
 * @param emTrial             assinaturas em TRIAL
 * @param trialsVencendo7Dias TRIAL com vencimento entre hoje e hoje+7
 * @param inadimplentes       assinaturas INADIMPLENTE
 * @param suspensas           assinaturas SUSPENSA
 * @param canceladas          assinaturas CANCELADA
 * @param cortesias           clínicas ativas em cortesia
 * @param mrr                 soma do valor mensal de ATIVA e INADIMPLENTE, sem cortesia
 * @param receitaMesAtual     cobranças PAGO com pagamento no mês atual
 * @param receitaMesAnterior  cobranças PAGO com pagamento no mês anterior
 * @param ticketMedio         MRR dividido pelas assinaturas pagantes
 * @param pendenciasManuais   eventos do Mercado Pago que exigem tratamento manual
 * @param usuariosAtivos      usuários ativos, sem o ROOT
 * @param consultasMesAtual   consultas do mês atual, exceto canceladas
 */
public record ResumoPlataformaResponse(
        long clinicasTotal,
        long clinicasAtivas,
        long emTrial,
        long trialsVencendo7Dias,
        long inadimplentes,
        long suspensas,
        long canceladas,
        long cortesias,
        BigDecimal mrr,
        BigDecimal receitaMesAtual,
        BigDecimal receitaMesAnterior,
        BigDecimal ticketMedio,
        long pendenciasManuais,
        long usuariosAtivos,
        long consultasMesAtual
) {
}
