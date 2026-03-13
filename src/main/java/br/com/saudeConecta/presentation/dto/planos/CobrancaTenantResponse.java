package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.CobrancaTenant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CobrancaTenantResponse(
        Long id,
        Long assinaturaId,
        Long organizacaoId,
        String organizacaoNome,
        String planoNome,
        BigDecimal valorTotal,
        String status,
        String pixCopiaECola,
        String pixQrcodeBase64,
        String txid,
        LocalDate dataVencimentoPix,
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
                entity.getPixCopiaECola(),
                entity.getPixQrcodeBase64(),
                entity.getTxid(),
                entity.getDataVencimentoPix(),
                entity.getDataPagamento(),
                entity.getCriadaEm()
        );
    }
}
