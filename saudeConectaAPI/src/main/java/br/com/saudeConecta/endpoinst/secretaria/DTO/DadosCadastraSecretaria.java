package br.com.saudeConecta.endpoinst.secretaria.DTO;

import java.sql.Date;

public record DadosCadastraSecretaria(


        String SecreNome,
        Byte SecreStatus,
        Date SecreDataCriacao,
        Long SecreUsuario,
        String SecreEmail


) {

}
