package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;

public record UsuarioResponse(
        Long id,
        String login,
        TipoUsuarioNovo tipoUsuarioNovo,
        StatusUsuario status
) {
    public UsuarioResponse(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getLogin(),
                usuario.getTipoUsuarioNovo(),
                usuario.getStatus()
        );
    }
}
