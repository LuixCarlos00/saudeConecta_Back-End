package br.com.saudeConecta.application.usecase.usuario;


import br.com.saudeConecta.domain.usuario.Usuario;

import java.util.List;

public interface BuscarTodosUsuariosUseCase {

    List<Usuario> executar();
}
