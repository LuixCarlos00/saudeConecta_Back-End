package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.secretaria.Secretaria;

/**
 * DTO para retornar secretária com ID do usuário.
 */
public record SecretariaComUsuarioResponse(
    Long secreCodigo,
    String secreNome,
    String secreEmail,
    Byte secreStatus,
    Long usuarioId,
    Byte usuarioStatus
) {
    public SecretariaComUsuarioResponse(Secretaria secretaria) {
        this(
            secretaria.getSecreCodigo(),
            secretaria.getSecreNome(),
            secretaria.getSecreEmail(),
            secretaria.getSecreStatus(),
            secretaria.getSecreUsuario() != null ? secretaria.getSecreUsuario().getId() : null,
            secretaria.getSecreUsuario() != null ? secretaria.getSecreUsuario().getStatus() : null
        );
    }
}
