package br.com.saudeConecta.endpoinst.administrador.DTO;

import java.sql.Date;

public record DadosCadastraAdministrador(


        String AdmNome,
        Byte AdmStatus,
        Date AdmDataCriacao,
        Long AdmUsuario,
        String AdmEmail,
        String AdmCodigoAtorizacao

) {

}
