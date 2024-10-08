package br.com.saudeConecta.endpoinst.endereco.DTO;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;

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
        return new Endereco(EndNacionalidade,EndUF,EndMunicipio,EndBairro,EndCep,EndRua,EndNumero,EndComplemento);
    }
}
