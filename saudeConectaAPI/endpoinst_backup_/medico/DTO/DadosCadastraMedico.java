package br.com.saudeConecta.endpoinst.medico.DTO;

import jakarta.validation.constraints.NotNull;

import java.sql.Date;

public record DadosCadastraMedico(

        @NotNull
        String MedNome,
        @NotNull
        String MedSexo,
        Date MedDataNacimento,
        @NotNull
        String MedCrm,

        String MedCpf,

        String MedRg,
        @NotNull
        String MedEspecialidade,
        @NotNull
        String MedEmail,


        String MedEmpresa,


        String MedGraduacao,


        String MedFormacoes,


        String MedTelefone,
        @NotNull
        Long usuario,
        @NotNull
        Long endereco

) {
}
