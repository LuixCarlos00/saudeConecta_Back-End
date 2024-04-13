package br.com.saudeConecta.endpoinst.usuario.DTO;

import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.jetbrains.annotations.NotNull;

public record DadosUsuarioView(
        Long id,
        String login  ,
        String senha


) {
    public DadosUsuarioView(@NotNull Usuario registro) {
        this(registro.getId(), registro.getLogin(), registro.getSenha() );
    }



}
