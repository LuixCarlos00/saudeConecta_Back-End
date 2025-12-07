package br.com.saudeConecta.application.usecase.medico.impl;

import br.com.saudeConecta.application.usecase.medico.BuscarMedicoPorIdUseCase;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarMedicoPorIdUseCaseImpl implements BuscarMedicoPorIdUseCase {

    private final MedicoRepository medicoRepository;

    @Override
    public Optional<Medico> executar(Long id) {
        log.debug("Buscando medico por ID de usuario: {}", id);
        return medicoRepository.findByUsuario_Id(id);
    }
}