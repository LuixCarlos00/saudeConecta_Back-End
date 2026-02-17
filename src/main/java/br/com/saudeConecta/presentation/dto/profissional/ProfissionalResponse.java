package br.com.saudeConecta.presentation.dto.profissional;

import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.presentation.dto.endereco.EnderecoResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProfissionalResponse(
        Long id,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String tipoProfissional,
        String conselho,

        @NotBlank(message = "Registro do conselho é obrigatório")
        String registroConselho,

        String sexo,
        String rg,
        LocalDate dataNascimento,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        String telefone,

        String formacao,
        String instituicao,

        BigDecimal valorConsulta,
        Integer tempoConsultaMinutos,
        String status,


        @NotNull(message = "Especialidades são obrigatórias")
        List<EspecialidadeResumo> especialidades,

        EnderecoResponse endereco
) {

    public static ProfissionalResponse fromEntity(Profissional p) {
        return new ProfissionalResponse(
                p.getId(),
                p.getNome(),
                p.getTipoProfissional() != null ? p.getTipoProfissional().getNome() : null,
                p.getTipoProfissional() != null ? p.getTipoProfissional().getConselho() : null,
                p.getRegistroConselho(),
                p.getSexo() != null ? p.getSexo().name() : null,
                p.getRg(),
                p.getDataNascimento(),
                p.getCpf(),
                p.getEmail(),
                p.getTelefone(),
                p.getFormacao(),
                p.getInstituicao(),
                p.getValorConsulta(),
                p.getTempoConsultaMinutos(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getEspecialidades().stream()
                        .map(e -> new EspecialidadeResumo(e.getId(), e.getNome()))
                        .toList(),
                p.getEndereco() != null ? new EnderecoResponse(p.getEndereco()) : null

        );
    }

    public record EspecialidadeResumo(Long id, String nome) {}
}