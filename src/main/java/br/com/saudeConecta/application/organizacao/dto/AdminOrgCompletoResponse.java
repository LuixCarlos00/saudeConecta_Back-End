package br.com.saudeConecta.application.organizacao.dto;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;

import java.util.function.Function;

/**
 * DTO de resposta com dados completos do AdminOrg + Organização + Endereço.
 * Usado pelo SUPER_ADMIN ao visualizar/editar um tenant.
 */
public record AdminOrgCompletoResponse(
    // Dados do Admin
    Long id,
    String nome,
    String cargo,
    String email,
    Boolean isOwner,
    String status,
    // Dados da Organização
    Long organizacaoId,
    String nomeClinica,
    String razaoSocial,
    String cnpj,
    String cpf,
    String tipoClinica,
    String emailClinica,
    String telefoneClinica,
    // Dados do Endereço
    Long enderecoId,
    String cep,
    String uf,
    String municipio,
    String bairro,
    String rua,
    Long numero,
    String complemento
) {
    public static AdminOrgCompletoResponse fromEntity(AdminOrganizacao admin) {
        Organizacao org = admin.getOrganizacao();
        Endereco end = valorOuNulo(org, Organizacao::getEndereco);

        // Se CNPJ for null ou vazio, retorna o CPF do usuário (login)
        String cnpj = valorOuNulo(org, Organizacao::getCnpj);
        String cpf = (cnpj == null || cnpj.isBlank()) ? valorOuNulo(admin.getUsuario(), Usuario::getLogin) : null;

        return new AdminOrgCompletoResponse(
            admin.getId(),
            admin.getNome(),
            admin.getCargo(),
            admin.getEmail(),
            admin.getIsOwner(),
            valorOuNulo(admin.getStatus(), Enum::name),
            // Organização
            valorOuNulo(org, Organizacao::getId),
            valorOuNulo(org, Organizacao::getNome),
            valorOuNulo(org, Organizacao::getRazaoSocial),
            cnpj,
            cpf,
            valorOuNulo(valorOuNulo(org, Organizacao::getTipo), Enum::name),
            valorOuNulo(org, Organizacao::getEmail),
            valorOuNulo(org, Organizacao::getTelefone),
            // Endereço
            valorOuNulo(end, Endereco::getEndCodigo),
            valorOuNulo(end, Endereco::getEndCep),
            valorOuNulo(end, Endereco::getEndUF),
            valorOuNulo(end, Endereco::getEndMunicipio),
            valorOuNulo(end, Endereco::getEndBairro),
            valorOuNulo(end, Endereco::getEndRua),
            valorOuNulo(end, Endereco::getEndNumero),
            valorOuNulo(end, Endereco::getEndComplemento)
        );
    }

    /** Aplica o extrator ao objeto, ou devolve nulo quando o objeto é nulo. */
    private static <T, R> R valorOuNulo(T origem, Function<T, R> extrator) {
        return origem != null ? extrator.apply(origem) : null;
    }
}
