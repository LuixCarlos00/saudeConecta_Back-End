package br.com.saudeConecta.endpoinst.usuario.DTO;

import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record DadosCadastraUsuario(
        String login  ,
        String senha


) {
    @Contract(value = " -> new", pure = true)
    public @NotNull Usuario toEntity() {
        return new Usuario(login, senha );
    }
}
