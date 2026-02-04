package br.com.saudeConecta.presentation.dto.medico;

import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.presentation.dto.endereco.EnderecoResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record MedicoResponse(
        Long medCodigo,
        String medNome,
        String medSexo,
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate medDataNacimento,
        String medCrm,
        String medCpf,
        String medRg,
        String medEmail,
        String medTelefone,
        String medEspecialidade,
        String medFormacoes,
        String medEmpresa,
        String medGraduacao,
        String medTempoDeConsulta,
        Long usuarioId,
        Long enderecoId,
        EnderecoResponse endereco
) {
    public MedicoResponse(Medico medico) {
        this(
                medico.getMedCodigo(),
                medico.getMedNome(),
                medico.getMedSexo(),
                medico.getMedDataNacimento() != null ? 
                    medico.getMedDataNacimento().toLocalDate() : null,
                medico.getMedCrm(),
                medico.getMedCpf(),
                medico.getMedRg(),
                medico.getMedEmail(),
                medico.getMedTelefone(),
                medico.getMedEspecialidade(),
                medico.getMedFormacoes(),
                medico.getMedEmpresa(),
                medico.getMedGraduacao(),
                medico.getMedTempoDeConsulta(),
                medico.getUsuario() != null ? 
                    medico.getUsuario().getId() : null,
                medico.getEndereco() != null ? 
                    medico.getEndereco().getEndCodigo() : null,
                medico.getEndereco() != null ? 
                    new EnderecoResponse(medico.getEndereco()) : null
        );
    }
}
