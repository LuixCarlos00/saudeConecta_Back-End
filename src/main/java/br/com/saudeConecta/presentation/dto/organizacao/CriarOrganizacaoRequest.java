package br.com.saudeConecta.presentation.dto.organizacao;

import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarOrganizacaoRequest(
    @NotBlank(message = "Nome é obrigatório")
    String nome,
    
    String razaoSocial,
    
    String cnpj,
    
    @NotNull(message = "Tipo é obrigatório")
    TipoOrganizacao tipo,
    
    String email,
    
    String telefone,
    
    String logoUrl,
    
    Long enderecoId
) {}
