package br.com.saudeConecta.application.plataforma.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Painel agregado da plataforma para o SUPER_ADMIN (contrato em CONTRATO-DASHBOARD-PLATAFORMA.md).
 * Sem dado pessoal: só nome da clínica, ids, contagens e valores.
 *
 * @param geradoEm          momento do cálculo (fuso de Brasília)
 * @param resumo            números dos cards
 * @param porStatus         clínicas por status de assinatura, inclusive os com 0
 * @param porPlano          clínicas ativas e MRR por plano base
 * @param serieMensal       série mensal terminando no mês atual, meses sem dado com 0
 * @param usuariosPorPerfil usuários ativos por perfil, sem o ROOT
 * @param topClinicasUso    até 10 clínicas ativas com mais consultas no mês
 * @param atencao           até 20 clínicas/eventos que precisam de ação
 */
public record DashboardPlataformaResponse(
        LocalDateTime geradoEm,
        ResumoPlataformaResponse resumo,
        List<StatusQuantidadeResponse> porStatus,
        List<PlanoResumoResponse> porPlano,
        List<SerieMensalResponse> serieMensal,
        List<PerfilQuantidadeResponse> usuariosPorPerfil,
        List<ClinicaUsoResponse> topClinicasUso,
        List<AtencaoResponse> atencao
) {
}
