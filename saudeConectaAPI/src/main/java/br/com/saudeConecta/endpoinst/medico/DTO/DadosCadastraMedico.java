package br.com.saudeConecta.endpoinst.medico.DTO;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
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
        @NotNull
        String MedCpf,
        @NotNull
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
//        public Medico toEntity() {
//                return new  Medico(MedNome,MedSexo,MedDataNacimento,MedCrm,MedCpf,MedRg ,MedTelefone,usuario,endereco);
//        }
}
