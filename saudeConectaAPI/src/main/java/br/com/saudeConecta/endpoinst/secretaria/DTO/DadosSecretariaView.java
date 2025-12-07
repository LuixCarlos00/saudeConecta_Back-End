package br.com.saudeConecta.endpoinst.secretaria.DTO;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosSecretariaView(Long SecreCodigo, String SecreNome, Usuario SecreUsuario   , Byte SecreStatus, Date SecreDataCriacao, String SecreEmail, String SecreCodigoAtorizacao)
{
    public DadosSecretariaView(@NotNull Secretaria registro) {
        this(registro.getSecreCodigo(), registro.getSecreNome(), registro.getSecreUsuario(), registro.getSecreStatus(),registro.getSecreDataCriacao() , registro.getSecreEmail() , registro.getSecreCodigoAtorizacao()); }



}
