package br.com.saudeConecta.presentation.dto.planos;

import br.com.saudeConecta.domain.planos.TipoPlano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

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

        BigDecimal valorAdicionalSecretaria,

        String titulo,

        List<String> recursos
) {

    /**
     * Formata a descrição no padrão esperado "titulo: xxx\nRecursos: [xxx, xxx]"
     * Se titulo e recursos forem fornecidos, usa o formatador.
     * Caso contrário, usa a descricao original (compatibilidade com versões antigas).
     *
     * @return descricao formatada
     */
    public String getDescricaoFormatada() {
        if (titulo != null && !titulo.trim().isEmpty() && recursos != null && !recursos.isEmpty()) {
            return PlanoDescricaoParser.format(titulo, recursos);
        }
        return descricao != null ? descricao : "";
    }
}
