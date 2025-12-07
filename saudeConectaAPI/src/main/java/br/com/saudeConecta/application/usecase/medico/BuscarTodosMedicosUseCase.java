package br.com.saudeConecta.application.usecase.medico;

import br.com.saudeConecta.domain.medico.Medico;

import java.util.List;

public interface BuscarTodosMedicosUseCase {
    List<Medico> executar();
}
