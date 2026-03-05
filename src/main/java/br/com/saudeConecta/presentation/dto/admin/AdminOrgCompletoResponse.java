package br.com.saudeConecta.presentation.dto.admin;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;

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
        Endereco end = org != null ? org.getEndereco() : null;

        return new AdminOrgCompletoResponse(
            admin.getId(),
            admin.getNome(),
            admin.getCargo(),
            admin.getEmail(),
            admin.getIsOwner(),
            admin.getStatus() != null ? admin.getStatus().name() : null,
            // Organização
            org != null ? org.getId() : null,
            org != null ? org.getNome() : null,
            org != null ? org.getRazaoSocial() : null,
            org != null ? org.getCnpj() : null,
            org != null && org.getTipo() != null ? org.getTipo().name() : null,
            org != null ? org.getEmail() : null,
            org != null ? org.getTelefone() : null,
            // Endereço
            end != null ? end.getEndCodigo() : null,
            end != null ? end.getEndCep() : null,
            end != null ? end.getEndUF() : null,
            end != null ? end.getEndMunicipio() : null,
            end != null ? end.getEndBairro() : null,
            end != null ? end.getEndRua() : null,
            end != null ? end.getEndNumero() : null,
            end != null ? end.getEndComplemento() : null
        );
    }
}
