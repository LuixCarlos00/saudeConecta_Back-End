package br.com.saudeConecta.application.paciente.dto;

import br.com.saudeConecta.application.common.dto.EnderecoResponse;
import br.com.saudeConecta.domain.paciente.Paciente;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record PacienteResponse(
        Long codigo,
        String nome,
        String sexo,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataNascimento,
        String cpf,
        String rg,
        String email,
        String telefone,
        String status,
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
                    new EnderecoResponse(paciente.getEndereco()) : null
        );
    }
}
