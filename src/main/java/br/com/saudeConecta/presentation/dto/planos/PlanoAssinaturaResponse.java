package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.PlanoAssinatura;

import java.math.BigDecimal;
import java.util.List;

public record PlanoAssinaturaResponse(
        Long id,
        String nome,
        String descricao,
        String tipo,
        BigDecimal valorMensal,
        Integer limiteAdminOrg,
        Integer limiteProfissional,
        Integer limiteSecretaria,
        BigDecimal valorAdicionalAdmin,
        BigDecimal valorAdicionalProfissional,
        BigDecimal valorAdicionalSecretaria,
        Boolean ativo,
        String titulo,
        List<String> recursos
) {

    /**
     * Converte a entidade PlanoAssinatura para o DTO de resposta.
     *
     * @param entity entidade PlanoAssinatura
     * @return PlanoAssinaturaResponse
     */
    public static PlanoAssinaturaResponse fromEntity(PlanoAssinatura entity) {
        PlanoDescricaoParser.PlanoDescricao parsed = PlanoDescricaoParser.parse(entity.getDescricao());
        
        return new PlanoAssinaturaResponse(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getTipo() != null ? entity.getTipo().name() : null,
                entity.getValorMensal(),
                entity.getLimiteAdminOrg(),
                entity.getLimiteProfissional(),
                entity.getLimiteSecretaria(),
                entity.getValorAdicionalAdmin(),
                entity.getValorAdicionalProfissional(),
                entity.getValorAdicionalSecretaria(),
                entity.getAtivo(),
                parsed.getTitulo(),
                parsed.getRecursos()
        );
    }
}
