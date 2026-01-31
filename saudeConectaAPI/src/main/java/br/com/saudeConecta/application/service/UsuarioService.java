package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.usuario.UsuarioInputPort;
import br.com.saudeConecta.application.port.out.usuario.UsuarioOutputPort;
import br.com.saudeConecta.domain.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService implements UsuarioInputPort {

    private final UsuarioOutputPort usuarioOutputPort;

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        log.debug("Buscando usuário por ID: {}", id);
        return usuarioOutputPort.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        log.debug("Buscando usuário por login: {}", login);
        return usuarioOutputPort.findUsuarioByLogin(login);
    }

    @Override
    public UserDetails buscarUserDetailsPorLogin(String login) {
        log.debug("Buscando UserDetails por login: {}", login);
        return usuarioOutputPort.findByLogin(login);
    }

    @Override
    public List<Usuario> buscarTodos() {
        log.debug("Buscando todos os usuários");
        return usuarioOutputPort.findAll();
    }

    @Override
    public Page<Usuario> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os usuários com paginação");
        return usuarioOutputPort.findAll(pageable);
    }

    @Override
    public boolean existePorLogin(String login) {
        log.debug("Verificando existência de usuário por login: {}", login);
        return usuarioOutputPort.existsByLogin(login);
    }

    @Override
    public Usuario cadastrar(Usuario usuario) {
        log.info("Cadastrando novo usuário: {}", usuario.getLogin());
        Usuario usuarioSalvo = usuarioOutputPort.save(usuario);
        log.info("Usuário cadastrado com sucesso. ID: {}", usuarioSalvo.getId());
        return usuarioSalvo;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão do usuário ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!usuarioOutputPort.existsById(id)) {
            log.warn("Usuário não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            usuarioOutputPort.deleteById(id);
            log.info("Usuário ID: {} excluído com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir usuário ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }
}
