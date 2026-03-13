package br.com.saudeConecta.presentation.dto.planos;

public record LimitesPlanoResponse(
        String planoNome,
        String planoTipo,

        Integer limiteAdminOrg,
        Integer limiteProfissional,
        Integer limiteSecretaria,

        Long usadoAdminOrg,
        Long usadoProfissional,
        Long usadoSecretaria,

        Boolean podeAdicionarAdminOrg,
        Boolean podeAdicionarProfissional,
        Boolean podeAdicionarSecretaria
) {

    /**
     * Cria uma resposta de limites a partir dos dados de uso e plano.
     *
     * @param planoNome           nome do plano
     * @param planoTipo           tipo do plano
     * @param limiteAdminOrg      limite de admins (null = ilimitado)
     * @param limiteProfissional  limite de profissionais (null = ilimitado)
     * @param limiteSecretaria    limite de secretárias (null = ilimitado)
     * @param usadoAdminOrg       quantidade atual de admins
     * @param usadoProfissional   quantidade atual de profissionais
     * @param usadoSecretaria     quantidade atual de secretárias
     * @return LimitesPlanoResponse
     */
    public static LimitesPlanoResponse of(
            String planoNome, String planoTipo,
            Integer limiteAdminOrg, Integer limiteProfissional, Integer limiteSecretaria,
            Long usadoAdminOrg, Long usadoProfissional, Long usadoSecretaria) {

        return new LimitesPlanoResponse(
                planoNome, planoTipo,
                limiteAdminOrg, limiteProfissional, limiteSecretaria,
                usadoAdminOrg, usadoProfissional, usadoSecretaria,
                limiteAdminOrg == null || usadoAdminOrg < limiteAdminOrg,
                limiteProfissional == null || usadoProfissional < limiteProfissional,
                limiteSecretaria == null || usadoSecretaria < limiteSecretaria
        );
    }
}
