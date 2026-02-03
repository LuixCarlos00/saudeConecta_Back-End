package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.medico.Medico;

/**
 * DTO para retornar médico com ID do usuário.
 */
public record MedicoComUsuarioResponse(
    Long medCodigo,
    String medNome,
    String medEmail,
    String medCrm,
    String medEspecialidade,
    String medTelefone,
    Long usuarioId,
    Byte usuarioStatus
) {
    public MedicoComUsuarioResponse(Medico medico) {
        this(
            medico.getMedCodigo(),
            medico.getMedNome(),
            medico.getMedEmail(),
            medico.getMedCrm(),
            medico.getMedEspecialidade(),
            medico.getMedTelefone(),
            medico.getUsuario() != null ? medico.getUsuario().getId() : null,
            medico.getUsuario() != null ? medico.getUsuario().getStatus() : null
        );
    }
}
