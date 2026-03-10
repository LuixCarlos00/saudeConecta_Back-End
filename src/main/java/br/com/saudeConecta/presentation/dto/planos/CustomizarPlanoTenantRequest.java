package br.com.saudeConecta.presentation.dto.planos;

import jakarta.validation.constraints.Min;

/**
 * Request para customizar limites do plano de um tenant específico.
 * Usado pelo SUPER_ADMIN para adicionar perfis extras além do plano base.
 *
 * @param limiteAdminOrgCustom      novo limite de admins (null = usar padrão do plano)
 * @param limiteProfissionalCustom  novo limite de profissionais (null = usar padrão do plano)
 * @param limiteSecretariaCustom    novo limite de secretárias (null = usar padrão do plano)
 */
public record CustomizarPlanoTenantRequest(

        @Min(value = 0, message = "Limite de administradores não pode ser negativo")
        Integer limiteAdminOrgCustom,

        @Min(value = 0, message = "Limite de profissionais não pode ser negativo")
        Integer limiteProfissionalCustom,

        @Min(value = 0, message = "Limite de secretárias não pode ser negativo")
        Integer limiteSecretariaCustom
) {
}
