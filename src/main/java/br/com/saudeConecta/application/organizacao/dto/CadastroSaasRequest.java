package br.com.saudeConecta.application.organizacao.dto;

import br.com.saudeConecta.config.web.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO do auto-cadastro público feito a partir da landing page.
 *
 * Reaproveita as mesmas regras do cadastro do SUPER_ADMIN, com duas diferenças:
 * o cargo é opcional e a flag de cortesia nunca é aceita do público.
 *
 * @param tipoPessoa   Tipo de pessoa (FISICA ou JURIDICA)
 * @param nome         Nome do administrador
 * @param cpf          CPF do administrador (login quando tipoPessoa=FISICA)
 * @param cargo        Cargo do administrador (opcional)
 * @param email        Email do administrador (também usado como pagador no Mercado Pago)
 * @param nomeClinica  Nome da organização (obrigatório para JURIDICA)
 * @param razaoSocial  Razão social (obrigatório para JURIDICA)
 * @param cnpj         CNPJ da organização (obrigatório para JURIDICA, usado como login)
 * @param tipoClinica  Tipo da organização (obrigatório para JURIDICA)
 * @param emailClinica Email da organização (obrigatório para JURIDICA)
 * @param telefone     Telefone de contato
 * @param cep          CEP do endereço
 * @param uf           UF do endereço
 * @param municipio    Município do endereço
 * @param bairro       Bairro do endereço
 * @param rua          Rua do endereço
 * @param numero       Número do endereço
 * @param complemento  Complemento do endereço (opcional)
 * @param planoId      ID do plano escolhido
 */
public record CadastroSaasRequest(

    @NotNull(message = "Tipo de pessoa é obrigatório")
    String tipoPessoa,

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @SafeInput
    String nome,

    @NotBlank(message = "CPF é obrigatório")
    @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
    String cpf,

    @Size(max = 50, message = "Cargo deve ter no máximo 50 caracteres")
    @SafeInput
    String cargo,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

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
    String complemento,

    @NotNull(message = "Plano é obrigatório")
    Long planoId

) {

    private static final String CARGO_PADRAO = "Responsável";

    /**
     * Converte para o request interno de cadastro completo, sempre sem cortesia.
     *
     * @return CadastrarAdminOrgCompletoRequest equivalente
     */
    public CadastrarAdminOrgCompletoRequest toAdminOrgRequest() {
        return new CadastrarAdminOrgCompletoRequest(
                tipoPessoa,
                nome,
                cpf,
                cargo == null || cargo.isBlank() ? CARGO_PADRAO : cargo,
                email,
                nomeClinica,
                razaoSocial,
                cnpj,
                tipoClinica,
                emailClinica,
                telefone,
                cep,
                uf,
                municipio,
                bairro,
                rua,
                numero,
                complemento,
                planoId,
                false);
    }
}
