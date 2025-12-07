package br.com.saudeConecta.application.usecase.administrador.impl;

import br.com.saudeConecta.application.usecase.administrador.DeletarAdministradorUseCase;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletarAdministradorUseCaseImpl implements DeletarAdministradorUseCase {

    private final AdministradorRepository administradorRepository;

    @Override
    @Transactional
    public void executar(Long id) throws Exception {
        log.info("Iniciando exclusao do administrador ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusao com ID invalido: {}", id);
            throw new IllegalArgumentException("ID invalido");
        }

        if (!administradorRepository.existsById(id)) {
            log.warn("Administrador nao encontrado para exclusao ID: {}", id);
            throw new Exception("Registro nao encontrado");
        }

        try {
            administradorRepository.deleteById(id);
            log.info("Administrador ID: {} excluido com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir administrador ID: {}", id, e);
            throw new Exception("Violacao de Integridade", e);
        }
    }
}
