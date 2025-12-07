package br.com.saudeConecta.application.usecase.administrador;

import br.com.saudeConecta.domain.administrador.Administrador;

import java.util.List;

public interface BuscarTodosAdministradoresUseCase {

    List<Administrador> executar();
}
