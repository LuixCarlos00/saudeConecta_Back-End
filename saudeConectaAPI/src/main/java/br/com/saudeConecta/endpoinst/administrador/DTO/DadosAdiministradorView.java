package br.com.saudeConecta.endpoinst.administrador.DTO;

import br.com.saudeConecta.endpoinst.administrador.Entity.Administrador;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public record DadosAdiministradorView( Long AdmCodigo,String AdmNome, Usuario AdmUsuario  ,Byte AdmStatus,Date AdmDataCriacao,String AdmEmail, String AdmCodigoAtorizacao)
{
    public DadosAdiministradorView(@NotNull Administrador registro) {
        this(registro.getAdmCodigo(), registro.getAdmNome(), registro.getAdmUsuario(), registro.getAdmStatus(),registro.getAdmDataCriacao() , registro.getAdmEmail() , registro.getAdmCodigoAtorizacao()); }



}
