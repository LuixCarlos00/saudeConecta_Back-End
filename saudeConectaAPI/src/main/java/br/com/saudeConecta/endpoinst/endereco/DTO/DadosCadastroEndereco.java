package br.com.saudeConecta.endpoinst.endereco.DTO;

import br.com.saudeConecta.domain.endereco.Endereco;

public record DadosCadastroEndereco(


        String EndNacionalidade,



        String EndUF,

        String EndMunicipio,

        String EndBairro,

        String EndCep,

        String EndRua,

        Long EndNumero,

        String EndComplemento
) {
    public Endereco toEntity() {
        return new Endereco();
    }
}
