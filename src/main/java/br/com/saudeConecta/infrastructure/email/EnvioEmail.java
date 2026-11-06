package br.com.saudeConecta.infrastructure.email;

/**
 * Dados de um envio de email HTML que passa pelo retry (e depois é registrado na mensageria).
 *
 * @param destinatario  email do destinatário
 * @param nome          nome do destinatário
 * @param assunto       assunto do email
 * @param corpoHtml     HTML já renderizado
 * @param tipoUsuario   tipo do usuário (para resolver TipoMensagem)
 * @param organizacaoId ID da organização (pode ser nulo)
 * @param entidadeId    ID da entidade destinatária (profissional, secretária, admin ou paciente)
 */
public record EnvioEmail(String destinatario, String nome, String assunto, String corpoHtml,
                         String tipoUsuario, Long organizacaoId, Long entidadeId) {
}
