package br.com.saudeConecta.presentation.dto.organizacao;

public record AtualizarOrganizacaoRequest(
    String nome,
    String razaoSocial,
    String cnpj,
    String email,
    String telefone,
    String logoUrl,
    Long enderecoId
) {}
