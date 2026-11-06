package br.com.saudeConecta.application.assinatura.dto;

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
        Integer limiteAdminOrgCustom,
        Integer limiteProfissionalCustom,
        Integer limiteSecretariaCustom,
        Integer qtdAdminExtra,
        Integer qtdProfissionalExtra,
        Integer qtdSecretariaExtra,
        BigDecimal valorAdicionalPerfis,
        String mpPreapprovalId,
        String mpStatus,
        String mpInitPoint,
        String mpPayerEmail,
        Boolean cortesia,
        LocalDateTime dataCancelamento,
        String canceladoPor,
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
                entity.getLimiteAdminOrgCustom(),
                entity.getLimiteProfissionalCustom(),
                entity.getLimiteSecretariaCustom(),
                entity.getQtdAdminExtra(),
                entity.getQtdProfissionalExtra(),
                entity.getQtdSecretariaExtra(),
                entity.getValorAdicionalPerfis(),
                entity.getMpPreapprovalId(),
                entity.getMpStatus(),
                entity.getMpInitPoint(),
                entity.getMpPayerEmail(),
                entity.getCortesia(),
                entity.getDataCancelamento(),
                entity.getCanceladoPor() != null ? entity.getCanceladoPor().name() : null,
                entity.getCriadoEm()
        );
    }
}
