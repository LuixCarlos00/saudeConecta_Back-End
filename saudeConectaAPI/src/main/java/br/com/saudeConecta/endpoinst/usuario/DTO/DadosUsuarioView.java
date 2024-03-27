package br.com.saudeConecta.endpoinst.usuario.DTO;

import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;

public record DadosUsuarioView(
        Long id,
        String login  ,
        String senha


) {
    public DadosUsuarioView(Usuario registro) {
        this(registro.getId(), registro.getLogin(), registro.getSenha() );
    }



}
