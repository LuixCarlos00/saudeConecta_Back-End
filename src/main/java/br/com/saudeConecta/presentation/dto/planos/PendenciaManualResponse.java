package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.MpWebhookEvento;

import java.time.LocalDateTime;

/**
 * Evento do Mercado Pago que precisa de tratamento manual pelo SUPER_ADMIN
 * (pagamento em assinatura cancelada, evento com erro expirado). O payload bruto não é exposto.
 *
 * @param eventoId    ID do evento em mp_webhook_evento
 * @param tipo        tópico notificado pelo Mercado Pago
 * @param dataId      identificador do recurso no Mercado Pago (fatura ou preapproval)
 * @param motivo      motivo da pendência (prefixo MOTIVO_* + contexto)
 * @param erro        último erro de processamento, se houver
 * @param tentativas  quantas vezes o processamento falhou
 * @param recebidoEm  quando a notificação chegou
 * @param expiradoEm  quando o reprocessamento automático desistiu (null se não expirou)
 */
public record PendenciaManualResponse(
        Long eventoId,
        String tipo,
        String dataId,
        String motivo,
        String erro,
        Integer tentativas,
        LocalDateTime recebidoEm,
        LocalDateTime expiradoEm
) {

    /**
     * @param evento evento marcado para atenção
     * @return a resposta correspondente
     */
    public static PendenciaManualResponse fromEntity(MpWebhookEvento evento) {
        return new PendenciaManualResponse(evento.getId(), evento.getTipo(), evento.getDataId(),
                evento.getMotivoAtencao(), evento.getErro(), evento.getTentativas(),
                evento.getRecebidoEm(), evento.getExpiradoEm());
    }
}
