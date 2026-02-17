package br.com.saudeConecta.presentation.dto.especialidade;

import br.com.saudeConecta.domain.profissional.Especialidade;

public record EspecialidadeResponse(
    Long id, 
    String nome, 
    String tipoProfissional,
    String tipoProfissionalCodigo
) {
    public static EspecialidadeResponse fromEntity(Especialidade e) {
        return new EspecialidadeResponse(
            e.getId(), 
            e.getNome(),
            e.getTipoProfissional() != null ? e.getTipoProfissional().getNome() : null,
            e.getTipoProfissional() != null ? e.getTipoProfissional().getCodigo() : null
        );
    }
}
