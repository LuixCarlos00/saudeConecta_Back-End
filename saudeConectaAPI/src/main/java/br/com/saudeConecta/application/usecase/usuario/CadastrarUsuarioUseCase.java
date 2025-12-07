package br.com.saudeConecta.application.usecase.usuario;


import br.com.saudeConecta.domain.usuario.Usuario;

public interface CadastrarUsuarioUseCase {

    Usuario executar(Usuario usuario);
}
