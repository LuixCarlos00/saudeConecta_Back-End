package br.com.saudeConecta.application.plataforma.dto;

import br.com.saudeConecta.domain.planos.TipoPlano;

import java.math.BigDecimal;

/**
 * @param plano      plano base (personalizados contam no plano de origem)
 * @param quantidade clínicas ativas no plano
 * @param mrr        MRR do plano (sem cortesia)
 */
public record PlanoResumoResponse(TipoPlano plano, long quantidade, BigDecimal mrr) {
}
