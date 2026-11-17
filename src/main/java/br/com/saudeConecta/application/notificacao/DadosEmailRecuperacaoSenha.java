package br.com.saudeConecta.application.notificacao;

/**
 * Dados do e-mail com o link de redefinição de senha.
 *
 * @param email           e-mail do destinatário
 * @param nome            nome exibido na saudação
 * @param login           login da conta a que o link se refere (útil quando o e-mail tem mais de uma conta)
 * @param link            URL da tela de redefinição, já com o token
 * @param validadeMinutos por quantos minutos o link vale
 * @param organizacaoId   ID da organização (nulo para o ROOT)
 */
public record DadosEmailRecuperacaoSenha(String email, String nome, String login, String link,
                                         long validadeMinutos, Long organizacaoId) {
}
