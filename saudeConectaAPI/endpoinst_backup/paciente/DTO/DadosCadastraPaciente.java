package br.com.saudeConecta.endpoinst.paciente.DTO;

import java.sql.Date;

public record DadosCadastraPaciente(

        String PaciNome,

        String PaciSexo,

        Date PaciDataNacimento,

        String PaciCpf,

        String PaciRg,

        String PaciEmail,

        String PaciTelefone,

        Long endereco,

        String PaciStatus

) {
}
