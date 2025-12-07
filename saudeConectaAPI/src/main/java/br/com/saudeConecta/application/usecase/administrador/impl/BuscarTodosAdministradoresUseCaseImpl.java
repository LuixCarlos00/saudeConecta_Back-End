package br.com.saudeConecta.application.usecase.administrador.impl;

import br.com.saudeConecta.application.usecase.administrador.BuscarTodosAdministradoresUseCase;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarTodosAdministradoresUseCaseImpl implements BuscarTodosAdministradoresUseCase {

    private final AdministradorRepository administradorRepository;

    @Override
    public List<Administrador> executar() {
        log.debug("Buscando todos os administradores");
        return administradorRepository.findAll();
    }
}
