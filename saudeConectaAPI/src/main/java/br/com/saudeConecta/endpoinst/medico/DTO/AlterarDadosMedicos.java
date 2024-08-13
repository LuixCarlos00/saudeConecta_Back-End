package br.com.saudeConecta.endpoinst.medico.DTO;


import lombok.Lombok;

import java.sql.Date;

public record AlterarDadosMedicos(

        String MedCpf,
        String MedCrm,

        Date MedDataNacimento,

        String MedEmail,

        String MedEspecialidade,

        String MedNome,

        String MedRg,

        String MedSexo,

        String MedTelefone,

        String MedTempoDeConsulta,

        Long Endereco,

        Long Usuario

) {
}
