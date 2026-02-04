package br.com.saudeConecta.presentation.dto.profissional;

import br.com.saudeConecta.domain.profissional.Profissional;

import java.time.LocalDate;
import java.util.List;

public record ProfissionalResponse(
    Long id,
    String nome,
    String tipoProfissional,
    String conselho,
    String registroConselho,
    String conselhoFormatado,
    String sexo,
    LocalDate dataNascimento,
    String cpf,
    String email,
    String telefone,
    String formacao,
    String instituicao,
    Integer tempoConsultaMinutos,
    String status,
    List<EspecialidadeResumo> especialidades
) {
    
    public static ProfissionalResponse fromEntity(Profissional p) {
        return new ProfissionalResponse(
            p.getId(),
            p.getNome(),
            p.getTipoProfissional() != null ? p.getTipoProfissional().getNome() : null,
            p.getTipoProfissional() != null ? p.getTipoProfissional().getConselho() : null,
            p.getRegistroConselho(),
            p.getConselhoFormatado(),
            p.getSexo() != null ? p.getSexo().name() : null,
            p.getDataNascimento(),
            p.getCpf(),
            p.getEmail(),
            p.getTelefone(),
            p.getFormacao(),
            p.getInstituicao(),
            p.getTempoConsultaMinutos(),
            p.getStatus() != null ? p.getStatus().name() : null,
            p.getEspecialidades().stream()
                .map(e -> new EspecialidadeResumo(e.getId(), e.getNome()))
                .toList()
        );
    }
    
    public record EspecialidadeResumo(Long id, String nome) {}
}
