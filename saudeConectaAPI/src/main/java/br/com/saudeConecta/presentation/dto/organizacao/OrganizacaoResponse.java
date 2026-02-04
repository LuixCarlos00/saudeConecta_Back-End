package br.com.saudeConecta.presentation.dto.organizacao;

import br.com.saudeConecta.domain.organizacao.Organizacao;

public record OrganizacaoResponse(
    Long id,
    String nome,
    String razaoSocial,
    String cnpj,
    String tipo,
    String email,
    String telefone,
    String logoUrl,
    String status
) {
    public static OrganizacaoResponse fromEntity(Organizacao org) {
        return new OrganizacaoResponse(
            org.getId(),
            org.getNome(),
            org.getRazaoSocial(),
            org.getCnpj(),
            org.getTipo() != null ? org.getTipo().name() : null,
            org.getEmail(),
            org.getTelefone(),
            org.getLogoUrl(),
            org.getStatus() != null ? org.getStatus().name() : null
        );
    }
}
