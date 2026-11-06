package br.com.saudeConecta.application.notificacao;

/**
 * Dados do email de confirmação de abertura de um chamado de suporte.
 *
 * @param email               email do usuário que abriu o chamado
 * @param nome                nome do usuário que abriu o chamado
 * @param protocolo           protocolo legível do chamado
 * @param titulo              título do chamado
 * @param corpo               descrição do chamado
 * @param categoria           descrição da categoria
 * @param prioridade          descrição da prioridade
 * @param status              descrição do status atual
 * @param prazoDias           prazo previsto em dias
 * @param previsaoAtendimento data prevista de atendimento formatada
 * @param quantidadeAnexos    quantidade de imagens anexadas
 * @param organizacaoId       ID da organização
 */
public record DadosEmailConfirmacaoChamado(String email, String nome, String protocolo, String titulo,
                                           String corpo, String categoria, String prioridade, String status,
                                           int prazoDias, String previsaoAtendimento, int quantidadeAnexos,
                                           Long organizacaoId) {
}
