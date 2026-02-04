package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.usuario.Usuario;

public record UsuarioResponse(
        Long id,
        String login,
        Byte tipoUsuario,
        Byte status
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
