package br.com.saudeConecta.presentation.dto.paciente;

import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.presentation.dto.endereco.EnderecoResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record PacienteResponse(
        Long paciCodigo,
        String paciNome,
        String paciSexo,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate paciDataNacimento,
        String paciCpf,
        String paciRg,
        String paciEmail,
        String paciTelefone,
        String paciStatus,
        Long enderecoId,
        EnderecoResponse endereco
) {
    public PacienteResponse(Paciente paciente) {
        this(
                paciente.getPaciCodigo(),
                paciente.getPaciNome(),
                paciente.getPaciSexo(),
                paciente.getPaciDataNacimento() != null ? 
                    paciente.getPaciDataNacimento().toLocalDate() : null,
                paciente.getPaciCpf(),
                paciente.getPaciRg(),
                paciente.getPaciEmail(),
                paciente.getPaciTelefone(),
                paciente.getPaciStatus(),
                paciente.getEndereco() != null ? 
                    paciente.getEndereco().getEndCodigo() : null,
                paciente.getEndereco() != null ? 
                    new EnderecoResponse(paciente.getEndereco()) : null
        );
    }
}
