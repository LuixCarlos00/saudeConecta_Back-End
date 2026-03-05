package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssinaturaTenantResponse(
        Long id,
        Long organizacaoId,
        String organizacaoNome,
        Long planoId,
        String planoNome,
        String planoTipo,
        String status,
        LocalDate dataInicio,
        LocalDate dataVencimento,
        LocalDate dataProximaCobranca,
        BigDecimal valorMensal,
        LocalDateTime criadoEm
) {

    /**
     * Converte a entidade AssinaturaTenant para o DTO de resposta.
     *
     * @param entity entidade AssinaturaTenant
     * @return AssinaturaTenantResponse
     */
    public static AssinaturaTenantResponse fromEntity(AssinaturaTenant entity) {
        return new AssinaturaTenantResponse(
                entity.getId(),
                entity.getOrganizacao() != null ? entity.getOrganizacao().getId() : null,
                entity.getOrganizacao() != null ? entity.getOrganizacao().getNome() : null,
                entity.getPlanoAssinatura() != null ? entity.getPlanoAssinatura().getId() : null,
                entity.getPlanoAssinatura() != null ? entity.getPlanoAssinatura().getNome() : null,
                entity.getPlanoAssinatura() != null ? entity.getPlanoAssinatura().getTipo().name() : null,
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getDataInicio(),
                entity.getDataVencimento(),
                entity.getDataProximaCobranca(),
                entity.getValorMensal(),
                entity.getCriadoEm()
        );
    }
}
