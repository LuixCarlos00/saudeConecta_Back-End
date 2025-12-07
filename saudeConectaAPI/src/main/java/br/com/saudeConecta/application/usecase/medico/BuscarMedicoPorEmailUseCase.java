package br.com.saudeConecta.application.usecase.medico;

import br.com.saudeConecta.domain.medico.Medico;

import java.util.Optional;

public interface BuscarMedicoPorEmailUseCase {
    Optional<Medico> executar(String email);
}
