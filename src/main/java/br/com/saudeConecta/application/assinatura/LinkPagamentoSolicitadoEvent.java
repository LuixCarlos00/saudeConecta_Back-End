package br.com.saudeConecta.application.assinatura;

/**
 * Pedido de geração e envio do link de pagamento de uma assinatura (ex.: depois do commit da
 * remoção da cortesia). Publicado por quem não pode depender do
 * {@link LinkPagamentoAssinaturaService} sem formar ciclo (o webhook depende do
 * {@code AssinaturaTenantService}); tratado de forma síncrona por esse serviço.
 *
 * @param assinaturaId ID da assinatura
 */
public record LinkPagamentoSolicitadoEvent(Long assinaturaId) {
}
