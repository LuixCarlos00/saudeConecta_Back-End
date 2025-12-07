package br.com.saudeConecta.application.usecase.medico.impl;

import br.com.saudeConecta.application.usecase.medico.BuscarMedicoPorEmailUseCase;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarMedicoPorEmailUseCaseImpl implements BuscarMedicoPorEmailUseCase {

    private final MedicoRepository medicoRepository;

    @Override
    public Optional<Medico> executar(String email) {
        log.debug("Buscando medico por email: {}", email);
        return medicoRepository.findByMedEmail(email);
    }
}
