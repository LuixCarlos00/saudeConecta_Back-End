package br.com.saudeConecta.application.notificacao;

import br.com.saudeConecta.domain.mensageria.TipoMensagem;

/**
 * Dados de um envio de email a ser registrado na mensageria (sucesso ou falha).
 *
 * @param organizacaoId ID da organização (pode ser nulo; nesse caso nada é registrado)
 * @param entidadeId    ID da entidade destinatária (profissional, secretária, admin ou paciente)
 * @param email         email do destinatário
 * @param nome          nome do destinatário
 * @param assunto       assunto do email
 * @param corpo         corpo da mensagem
 * @param tipo          tipo da mensagem
 */
public record RegistroEnvioEmail(Long organizacaoId, Long entidadeId, String email,
                                 String nome, String assunto, String corpo, TipoMensagem tipo) {
}
