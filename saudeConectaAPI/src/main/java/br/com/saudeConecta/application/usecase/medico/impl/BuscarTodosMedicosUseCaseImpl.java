package br.com.saudeConecta.application.usecase.medico.impl;

import br.com.saudeConecta.application.usecase.medico.BuscarTodosMedicosUseCase;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarTodosMedicosUseCaseImpl implements BuscarTodosMedicosUseCase {

    private final MedicoRepository medicoRepository;

    @Override
    public List<Medico> executar() {
        log.debug("Buscando todos os medicos");
        return medicoRepository.findAll();
    }
}
