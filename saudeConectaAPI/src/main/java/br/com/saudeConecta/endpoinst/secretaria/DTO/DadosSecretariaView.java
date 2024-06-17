package br.com.saudeConecta.endpoinst.secretaria.DTO;

import br.com.saudeConecta.endpoinst.secretaria.Entity.Secretaria;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosSecretariaView(Long SecreCodigo, String SecreNome, Usuario SecreUsuario   , Byte SecreStatus, Date SecreDataCriacao, String SecreEmail)
{
    public DadosSecretariaView(@NotNull Secretaria registro) {
        this(registro.getSecreCodigo(), registro.getSecreNome(), registro.getSecreUsuario(), registro.getSecreStatus(),registro.getSecreDataCriacao() , registro.getSecreEmail()); }



}
