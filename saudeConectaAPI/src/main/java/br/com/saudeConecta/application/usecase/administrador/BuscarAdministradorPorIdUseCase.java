package br.com.saudeConecta.application.usecase.administrador;

import br.com.saudeConecta.domain.administrador.Administrador;

import java.util.Optional;

public interface BuscarAdministradorPorIdUseCase {

    Optional<Administrador> executar(Long id);
}
