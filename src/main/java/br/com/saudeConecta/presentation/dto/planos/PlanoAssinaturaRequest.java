package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.TipoPlano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlanoAssinaturaRequest(

        @NotBlank(message = "Nome do plano é obrigatório")
        String nome,

        String descricao,

        @NotNull(message = "Tipo do plano é obrigatório")
        TipoPlano tipo,

        @NotNull(message = "Valor mensal é obrigatório")
        @Positive(message = "Valor mensal deve ser positivo")
        BigDecimal valorMensal,

        Integer limiteAdminOrg,

        Integer limiteProfissional,

        Integer limiteSecretaria,

        BigDecimal valorAdicionalAdmin,

        BigDecimal valorAdicionalProfissional,

        BigDecimal valorAdicionalSecretaria
) {
}
