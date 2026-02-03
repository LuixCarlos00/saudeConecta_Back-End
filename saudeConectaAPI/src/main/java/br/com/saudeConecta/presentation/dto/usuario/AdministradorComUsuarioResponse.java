package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.administrador.Administrador;

/**
 * DTO para retornar administrador com ID do usuário.
 */
public record AdministradorComUsuarioResponse(
    Long admCodigo,
    String admNome,
    String admEmail,
    Byte admStatus,
    Long usuarioId,
    Byte usuarioStatus
) {
    public AdministradorComUsuarioResponse(Administrador admin) {
        this(
            admin.getAdmCodigo(),
            admin.getAdmNome(),
            admin.getAdmEmail(),
            admin.getAdmStatus(),
            admin.getAdmUsuario() != null ? admin.getAdmUsuario().getId() : null,
            admin.getAdmUsuario() != null ? admin.getAdmUsuario().getStatus() : null
        );
    }
}
