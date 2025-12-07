package br.com.saudeConecta.application.usecase.usuario.impl;

import br.com.saudeConecta.application.usecase.usuario.CadastrarUsuarioUseCase;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarUsuarioUseCaseImpl implements CadastrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Usuario executar(Usuario usuario) {
        log.info("Cadastrando novo usuario: {}", usuario.getLogin());
        
        String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
        usuario.setSenha(senhaCriptografada);
        
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuario cadastrado com sucesso. ID: {}", usuarioSalvo.getId());
        return usuarioSalvo;
    }
}
