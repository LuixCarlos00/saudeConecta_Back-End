package br.com.saudeConecta.application.usecase.administrador.impl;

import br.com.saudeConecta.application.usecase.administrador.CadastrarAdministradorUseCase;
import br.com.saudeConecta.domain.administrador.Administrador;
import br.com.saudeConecta.infrastructure.persistence.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarAdministradorUseCaseImpl implements CadastrarAdministradorUseCase {

    private final AdministradorRepository administradorRepository;

    @Override
    public Administrador executar(Administrador administrador) {
        log.info("Cadastrando novo administrador: {}", administrador.getAdmNome());
        Administrador administradorSalvo = administradorRepository.save(administrador);
        log.info("Administrador cadastrado com sucesso. ID: {}", administradorSalvo.getAdmCodigo());
        return administradorSalvo;
    }
}
