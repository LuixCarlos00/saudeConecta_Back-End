package br.com.saudeConecta.application.plataforma.dto;

import br.com.saudeConecta.domain.planos.StatusAssinatura;

/**
 * @param status     status da assinatura
 * @param quantidade clínicas nesse status
 */
public record StatusQuantidadeResponse(StatusAssinatura status, long quantidade) {
}
