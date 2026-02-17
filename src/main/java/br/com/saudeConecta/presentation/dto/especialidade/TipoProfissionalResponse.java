package br.com.saudeConecta.presentation.dto.especialidade;

import br.com.saudeConecta.domain.profissional.TipoProfissional;

public record TipoProfissionalResponse(
    Long id, 
    String codigo, 
    String nome, 
    String conselho
) {
    public static TipoProfissionalResponse fromEntity(TipoProfissional t) {
        return new TipoProfissionalResponse(
            t.getId(),
            t.getCodigo(),
            t.getNome(),
            t.getConselho()
        );
    }
}
