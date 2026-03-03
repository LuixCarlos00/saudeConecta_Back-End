package br.com.saudeConecta.presentation.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
    String nome,

    @NotBlank(message = "CPF é obrigatório")
    String cpf,

    @NotBlank(message = "Cargo é obrigatório")
    String cargo,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String email,

    @NotBlank(message = "Nome da clínica é obrigatório")
    String nomeClinica,

    @NotBlank(message = "Razão social é obrigatória")
    String razaoSocial,

    @NotBlank(message = "CNPJ é obrigatório")
    String cnpj,

    @NotBlank(message = "Tipo da clínica é obrigatório")
    String tipoClinica,

    @NotBlank(message = "Email da clínica é obrigatório")
    @Email(message = "Email da clínica inválido")
    String emailClinica,

    String telefone,

    @NotBlank(message = "CEP é obrigatório")
    String cep,

    @NotBlank(message = "UF é obrigatória")
    String uf,

    @NotBlank(message = "Município é obrigatório")
    String municipio,

    String bairro,

    @NotBlank(message = "Rua é obrigatória")
    String rua,

    @NotNull(message = "Número é obrigatório")
    Long numero,

    String complemento

) {}
