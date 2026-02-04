package br.com.saudeConecta.presentation.dto.endereco;

import br.com.saudeConecta.domain.endereco.Endereco;

public record EnderecoResponse(
        Long endCodigo,
        String endNacionalidade,
        String endUF,
        String endMunicipio,
        String endBairro,
        String endCep,
        String endRua,
        Long endNumero,
        String endComplemento
) {
    public EnderecoResponse(Endereco endereco) {
        this(
                endereco.getEndCodigo(),
                endereco.getEndNacionalidade(),
                endereco.getEndUF(),
                endereco.getEndMunicipio(),
                endereco.getEndBairro(),
                endereco.getEndCep(),
                endereco.getEndRua(),
                endereco.getEndNumero(),
                endereco.getEndComplemento()
        );
    }
}
