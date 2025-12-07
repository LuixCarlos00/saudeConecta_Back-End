package br.com.saudeConecta.application.usecase.administrador.impl;

import br.com.saudeConecta.application.usecase.administrador.BuscarAdministradorPorIdUseCase;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarAdministradorPorIdUseCaseImpl implements BuscarAdministradorPorIdUseCase {

    private final AdministradorRepository administradorRepository;

    @Override
    public Optional<Administrador> executar(Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        return administradorRepository.findById(id);
    }
}
