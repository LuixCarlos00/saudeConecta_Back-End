package br.com.saudeConecta.presentation.dto.admin;

/**
 * DTO para atualização completa de Admin Org pelo SUPER_ADMIN.
 * Atualiza simultaneamente: AdminOrganizacao, Organizacao e Endereco.
 *
 * @param nome           Nome do administrador
 * @param cargo          Cargo do administrador
 * @param email          Email do administrador
 * @param nomeClinica    Nome da organização/clínica
 * @param razaoSocial    Razão social da organização
 * @param cnpj           CNPJ da organização
 * @param tipoClinica    Tipo da organização
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
public record AtualizarAdminOrgCompletoRequest(
    String nome,
    String cargo,
    String email,
    // Dados da Organização
    String nomeClinica,
    String razaoSocial,
    String cnpj,
    String tipoClinica,
    String emailClinica,
    String telefone,
    // Dados do Endereço
    String cep,
    String uf,
    String municipio,
    String bairro,
    String rua,
    Long numero,
    String complemento
) {}
