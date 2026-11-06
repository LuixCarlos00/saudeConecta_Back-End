package br.com.saudeConecta.application.plataforma.dto;

import br.com.saudeConecta.domain.planos.StatusAssinatura;
import br.com.saudeConecta.domain.planos.TipoPlano;

/**
 * @param organizacaoId id da clínica
 * @param nome          nome da clínica
 * @param plano         plano base
 * @param status        status da assinatura
 * @param consultasMes  consultas do mês atual, exceto canceladas
 * @param profissionais profissionais ativos da clínica
 */
public record ClinicaUsoResponse(Long organizacaoId, String nome, TipoPlano plano, StatusAssinatura status,
                                 long consultasMes, long profissionais) {
}
