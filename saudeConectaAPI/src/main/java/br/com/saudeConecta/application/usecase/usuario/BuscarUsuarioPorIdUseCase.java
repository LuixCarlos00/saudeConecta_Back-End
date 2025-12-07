package br.com.saudeConecta.application.usecase.usuario;

import br.com.saudeConecta.domain.usuario.Usuario;

import java.util.Optional;

public interface BuscarUsuarioPorIdUseCase {

    Optional<Usuario> executar(Long id);
}
