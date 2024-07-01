package br.com.saudeConecta.endpoinst.endereco.DTO;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;

public record DadosEnderecoView(Long EndCodigo, String EndNacionalidade,  String EndUF, String EndMunicipio,
                                String EndBairro, String EndCep, String EndRua, Long EndNumero, String EndComplemento) {


    public DadosEnderecoView(Endereco registro) {
        this(registro.getEndCodigo(),registro.getEndNacionalidade() ,registro.getEndUF(),
                registro.getEndMunicipio(),registro.getEndBairro(),registro.getEndCep(),registro.getEndRua(),
                registro.getEndNumero(),registro.getEndComplemento());
    }
}
