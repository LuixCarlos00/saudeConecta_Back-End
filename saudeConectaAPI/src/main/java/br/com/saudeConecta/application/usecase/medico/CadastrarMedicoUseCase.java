package br.com.saudeConecta.application.usecase.medico;

import br.com.saudeConecta.domain.medico.Medico;

public interface CadastrarMedicoUseCase {
    Medico executar(Medico medico);
}
