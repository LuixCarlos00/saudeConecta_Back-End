package br.com.saudeConecta.endpoinst.paciente.DTO;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.paciente.Entity.Paciente;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import lombok.Lombok;

import java.sql.Date;

public record DadosCadastraPaciente(

        String PaciNome,

        String PaciSexo,

        Date PaciDataNacimento,

        String PaciCpf,

        String PaciRg,

        String PaciEmail,

        //Long usuario,

        String PaciTelefone,

        Long endereco,

        String PaciStatus


) {
//    public Paciente toEntity() {
//        return new Paciente(PaciNome, PaciSexo, PaciDataNacimento, PaciCpf, PaciRg, usuario, PaciTelefone,  endereco);
//    }
}
