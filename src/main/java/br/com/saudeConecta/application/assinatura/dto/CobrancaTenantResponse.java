package br.com.saudeConecta.application.assinatura.dto;

import br.com.saudeConecta.domain.planos.CobrancaTenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CobrancaTenantResponse(
        Long id,
        Long assinaturaId,
        Long organizacaoId,
        String organizacaoNome,
        String planoNome,
        BigDecimal valorTotal,
        String status,
        String formaPagamento,
        String mpPaymentId,
        String motivoRecusa,
        String txid,
        LocalDateTime dataPagamento,
        LocalDateTime criadaEm
) {

    /**
     * Converte a entidade CobrancaTenant para o DTO de resposta.
     *
     * @param entity entidade CobrancaTenant
     * @return CobrancaTenantResponse
     */
    public static CobrancaTenantResponse fromEntity(CobrancaTenant entity) {
        return new CobrancaTenantResponse(
                entity.getId(),
                entity.getAssinaturaTenant() != null ? entity.getAssinaturaTenant().getId() : null,
                entity.getOrganizacao() != null ? entity.getOrganizacao().getId() : null,
                entity.getOrganizacao() != null ? entity.getOrganizacao().getNome() : null,
                entity.getAssinaturaTenant() != null && entity.getAssinaturaTenant().getPlanoAssinatura() != null
                        ? entity.getAssinaturaTenant().getPlanoAssinatura().getNome() : null,
                entity.getValorTotal(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getFormaPagamento() != null ? entity.getFormaPagamento().name() : null,
                entity.getMpPaymentId(),
                entity.getMotivoRecusa(),
                entity.getTxid(),
                entity.getDataPagamento(),
                entity.getCriadaEm()
        );
    }
}
