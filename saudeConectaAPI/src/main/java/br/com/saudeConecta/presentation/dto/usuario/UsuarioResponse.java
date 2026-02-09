package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;

public record UsuarioResponse(
        Long id,
        String login,
        Byte tipoUsuario,
        StatusUsuario status
) {
    public UsuarioResponse(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getTipoUsuario(),
                usuario.getStatus()
        );
    }
}
