package br.com.saudeConecta.presentation.dto.usuario;

import br.com.saudeConecta.domain.paciente.Paciente;

/**
 * DTO para retornar paciente.
 */
public record PacienteResponse(
    Long paciCodigo,
    String paciNome,
    String paciEmail,
    String paciTelefone,
    String paciCpf,
    String paciStatus
) {
    public PacienteResponse(Paciente paciente) {
        this(
            paciente.getPaciCodigo(),
            paciente.getPaciNome(),
            paciente.getPaciEmail(),
            paciente.getPaciTelefone(),
            paciente.getPaciCpf(),
            paciente.getPaciStatus()
        );
    }
}
