package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.PlanoAssinatura;

import java.math.BigDecimal;

public record PlanoAssinaturaResponse(
        Long id,
        String nome,
        String descricao,
        String tipo,
        BigDecimal valorMensal,
        Integer limiteAdminOrg,
        Integer limiteProfissional,
        Integer limiteSecretaria,
        Boolean ativo
) {

    /**
     * Converte a entidade PlanoAssinatura para o DTO de resposta.
     *
     * @param entity entidade PlanoAssinatura
     * @return PlanoAssinaturaResponse
     */
    public static PlanoAssinaturaResponse fromEntity(PlanoAssinatura entity) {
        return new PlanoAssinaturaResponse(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getTipo() != null ? entity.getTipo().name() : null,
                entity.getValorMensal(),
                entity.getLimiteAdminOrg(),
                entity.getLimiteProfissional(),
                entity.getLimiteSecretaria(),
                entity.getAtivo()
        );
    }
}
