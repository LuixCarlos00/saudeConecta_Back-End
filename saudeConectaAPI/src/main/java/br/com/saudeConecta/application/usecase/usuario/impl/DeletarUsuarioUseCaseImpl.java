package br.com.saudeConecta.application.usecase.usuario.impl;

import br.com.saudeConecta.application.usecase.usuario.DeletarUsuarioUseCase;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletarUsuarioUseCaseImpl implements DeletarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void executar(Long id) throws Exception {
        log.info("Iniciando exclusao do usuario ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusao com ID invalido: {}", id);
            throw new IllegalArgumentException("ID invalido");
        }

        if (!usuarioRepository.existsById(id)) {
            log.warn("Usuario nao encontrado para exclusao ID: {}", id);
            throw new Exception("Registro nao encontrado");
        }

        try {
            usuarioRepository.deleteById(id);
            log.info("Usuario ID: {} excluido com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir usuario ID: {}", id, e);
            throw new Exception("Violacao de Integridade", e);
        }
    }
}
