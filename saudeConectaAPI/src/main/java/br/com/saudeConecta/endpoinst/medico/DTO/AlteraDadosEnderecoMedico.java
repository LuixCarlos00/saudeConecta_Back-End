package br.com.saudeConecta.endpoinst.medico.DTO;


public record AlteraDadosEnderecoMedico(

        String EndBairro,
        String EndCep,
        String EndComplemento,
        String EndMunicipio,
        String EndNacionalidade,
        Long EndNumero,
        String EndRua,
        String EndUF,
        Long endCodigo // Alterado para Long
) {
}

