package br.com.saudeConecta.application.usecase.consulta.impl;

import br.com.saudeConecta.application.usecase.consulta.BuscarConsultaPorIdUseCase;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarConsultaPorIdUseCaseImpl implements BuscarConsultaPorIdUseCase {

    private final ConsultaRepository consultaRepository;

    @Override
    public Optional<Consulta> executar(Long id) {
        log.debug("Buscando consulta por ID: {}", id);
        return consultaRepository.findById(id);
    }
}
