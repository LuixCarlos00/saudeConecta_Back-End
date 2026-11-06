package br.com.saudeConecta.application.notificacao;

/**
 * Dados do email que avisa o autor de um chamado sobre a mudança de status.
 *
 * @param email          email do autor do chamado
 * @param nome           nome do autor do chamado
 * @param protocolo      protocolo legível do chamado
 * @param titulo         título do chamado
 * @param statusAnterior descrição do status anterior
 * @param statusAtual    descrição do novo status
 * @param observacao     mensagem opcional da equipe de suporte
 * @param organizacaoId  ID da organização
 */
public record DadosEmailAtualizacaoStatusChamado(String email, String nome, String protocolo, String titulo,
                                                 String statusAnterior, String statusAtual, String observacao,
                                                 Long organizacaoId) {
}
