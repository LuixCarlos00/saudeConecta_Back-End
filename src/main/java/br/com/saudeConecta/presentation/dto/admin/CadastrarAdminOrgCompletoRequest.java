package br.com.saudeConecta.presentation.dto.admin;

import br.com.saudeConecta.util.validation.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para cadastro completo de Admin de Organização pelo SUPER_ADMIN.
 * Cria simultaneamente: Endereco, Organizacao, Usuario e AdminOrganizacao.
 * A senha do usuário é o próprio CPF informado.
 *
 * @param nome           Nome do administrador
 * @param cpf            CPF do administrador (usado como login e senha inicial)
 * @param cargo          Cargo do administrador (GERENTE, DIRETOR_ADMINISTRATIVO)
 * @param email          Email do administrador
 * @param nomeClinica    Nome da organização/clínica
 * @param razaoSocial    Razão social da organização
 * @param cnpj           CNPJ da organização
 * @param tipoClinica    Tipo da organização (CLINICA, CONSULTORIO, etc.)
 * @param emailClinica   Email da organização
 * @param telefone       Telefone da organização
 * @param cep            CEP do endereço
 * @param uf             UF do endereço
 * @param municipio      Município do endereço
 * @param bairro         Bairro do endereço
 * @param rua            Rua do endereço
 * @param numero         Número do endereço
 * @param complemento    Complemento do endereço (opcional)
 */
public record CadastrarAdminOrgCompletoRequest(

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @SafeInput
    String nome,

    @NotBlank(message = "CPF é obrigatório")
    @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
    String cpf,

    @NotBlank(message = "Cargo é obrigatório")
    @Size(max = 50, message = "Cargo deve ter no máximo 50 caracteres")
    @SafeInput
    String cargo,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

    @NotBlank(message = "Nome da clínica é obrigatório")
    @Size(max = 100, message = "Nome da clínica deve ter no máximo 100 caracteres")
    @SafeInput
    String nomeClinica,

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 150, message = "Razão social deve ter no máximo 150 caracteres")
    @SafeInput
    String razaoSocial,

    @NotBlank(message = "CNPJ é obrigatório")
    @Size(max = 18, message = "CNPJ deve ter no máximo 18 caracteres")
    String cnpj,

    @NotBlank(message = "Tipo da clínica é obrigatório")
    @Size(max = 30, message = "Tipo da clínica deve ter no máximo 30 caracteres")
    String tipoClinica,

    @NotBlank(message = "Email da clínica é obrigatório")
    @Email(message = "Email da clínica inválido")
    @Size(max = 100, message = "Email da clínica deve ter no máximo 100 caracteres")
    String emailClinica,

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String telefone,

    @NotBlank(message = "CEP é obrigatório")
    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    String cep,

    @NotBlank(message = "UF é obrigatória")
    @Size(max = 2, message = "UF deve ter no máximo 2 caracteres")
    String uf,

    @NotBlank(message = "Município é obrigatório")
    @Size(max = 100, message = "Município deve ter no máximo 100 caracteres")
    @SafeInput
    String municipio,

    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    @SafeInput
    String bairro,

    @NotBlank(message = "Rua é obrigatória")
    @Size(max = 200, message = "Rua deve ter no máximo 200 caracteres")
    @SafeInput
    String rua,

    @NotNull(message = "Número é obrigatório")
    Long numero,

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    @SafeInput
    String complemento

) {}
