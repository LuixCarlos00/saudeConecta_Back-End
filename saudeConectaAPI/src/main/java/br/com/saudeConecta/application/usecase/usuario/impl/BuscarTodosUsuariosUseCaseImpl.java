package br.com.saudeConecta.application.usecase.usuario.impl;

import br.com.saudeConecta.application.usecase.usuario.BuscarTodosUsuariosUseCase;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarTodosUsuariosUseCaseImpl implements BuscarTodosUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;

    @Override
    public List<Usuario> executar() {
        log.debug("Buscando todos os usuarios");
        return usuarioRepository.findAll();
    }
}
