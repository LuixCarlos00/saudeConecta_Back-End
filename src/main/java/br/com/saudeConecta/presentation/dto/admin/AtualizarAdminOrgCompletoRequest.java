package br.com.saudeConecta.presentation.dto.admin;

import br.com.saudeConecta.util.validation.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização completa de Admin Org pelo SUPER_ADMIN.
 * Atualiza simultaneamente: AdminOrganizacao, Organizacao e Endereco.
 */
public record AtualizarAdminOrgCompletoRequest(
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @SafeInput
    String nome,

    @Size(max = 50, message = "Cargo deve ter no máximo 50 caracteres")
    @SafeInput
    String cargo,

    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

    // Dados da Organização
    @Size(max = 100, message = "Nome da clínica deve ter no máximo 100 caracteres")
    @SafeInput
    String nomeClinica,

    @Size(max = 150, message = "Razão social deve ter no máximo 150 caracteres")
    @SafeInput
    String razaoSocial,

    @Size(max = 18, message = "CNPJ deve ter no máximo 18 caracteres")
    String cnpj,

    @Size(max = 30, message = "Tipo da clínica deve ter no máximo 30 caracteres")
    String tipoClinica,

    @Email(message = "Email da clínica inválido")
    @Size(max = 100, message = "Email da clínica deve ter no máximo 100 caracteres")
    String emailClinica,

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String telefone,

    // Dados do Endereço
    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    String cep,

    @Size(max = 2, message = "UF deve ter no máximo 2 caracteres")
    String uf,

    @Size(max = 100, message = "Município deve ter no máximo 100 caracteres")
    @SafeInput
    String municipio,

    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    @SafeInput
    String bairro,

    @Size(max = 200, message = "Rua deve ter no máximo 200 caracteres")
    @SafeInput
    String rua,

    Long numero,

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    @SafeInput
    String complemento
) {}
