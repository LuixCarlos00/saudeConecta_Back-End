package br.com.saudeConecta.application.usecase.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;

import java.util.Optional;

public interface BuscarConsultaPorIdUseCase {

    Optional<Consulta> executar(Long id);
}
