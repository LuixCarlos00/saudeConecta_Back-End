package br.com.saudeConecta.application.assinatura.dto;

import br.com.saudeConecta.domain.planos.MpWebhookEvento;

import java.time.LocalDateTime;

/**
 * Evento do Mercado Pago que precisa (ou precisou) de tratamento manual pelo SUPER_ADMIN.
 * O payload bruto não é exposto.
 *
 * @param eventoId             ID do evento em mp_webhook_evento (usado no "resolver")
 * @param tipo                 tópico notificado pelo Mercado Pago, ou pendencia_preapproval_substituido
 * @param dataId               identificador do recurso no Mercado Pago (fatura ou preapproval)
 * @param motivoCodigo         código do motivo: PAGAMENTO_EM_CANCELADA, PAGAMENTO_EM_CORTESIA,
 *                             PAGAMENTO_SEM_ASSINATURA, PREAPPROVAL_SUBSTITUIDO_AUTORIZADO ou EVENTO_EXPIRADO
 * @param motivo               texto completo do motivo (código + contexto)
 * @param erro                 último erro de processamento, se houver
 * @param tentativas           quantas vezes o processamento falhou
 * @param recebidoEm           quando a notificação chegou (ou a pendência foi registrada)
 * @param expiradoEm           quando o reprocessamento automático desistiu (null se não expirou)
 * @param organizacaoId        clínica da pendência (null se desconhecida)
 * @param organizacaoNome      nome da clínica (null se desconhecida)
 * @param resolvida            true se o SUPER_ADMIN já marcou como resolvida
 * @param resolvidoEm          quando foi resolvida
 * @param resolvidoPorUsuarioId quem resolveu
 * @param observacaoResolucao  o que foi feito
 */
public record PendenciaManualResponse(
        Long eventoId,
        String tipo,
        String dataId,
        String motivoCodigo,
        String motivo,
        String erro,
        Integer tentativas,
        LocalDateTime recebidoEm,
        LocalDateTime expiradoEm,
        Long organizacaoId,
        String organizacaoNome,
        boolean resolvida,
        LocalDateTime resolvidoEm,
        Long resolvidoPorUsuarioId,
        String observacaoResolucao
) {

    /**
     * @param evento          evento marcado para atenção
     * @param organizacaoNome nome da clínica do evento (null se desconhecida)
     * @return a resposta correspondente
     */
    public static PendenciaManualResponse fromEntity(MpWebhookEvento evento, String organizacaoNome) {
        return new PendenciaManualResponse(evento.getId(), evento.getTipo(), evento.getDataId(),
                evento.getMotivoCodigo(), evento.getMotivoAtencao(), evento.getErro(), evento.getTentativas(),
                evento.getRecebidoEm(), evento.getExpiradoEm(), evento.getOrganizacaoId(), organizacaoNome,
                evento.getResolvidoEm() != null, evento.getResolvidoEm(), evento.getResolvidoPorUsuarioId(),
                evento.getObservacaoResolucao());
    }
}
