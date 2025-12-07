package br.com.saudeConecta.application.usecase.consulta.impl;

import br.com.saudeConecta.application.usecase.consulta.BuscarTodasConsultasUseCase;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuscarTodasConsultasUseCaseImpl implements BuscarTodasConsultasUseCase {

    private final ConsultaRepository consultaRepository;

    @Override
    public List<Consulta> executar() {
        log.debug("Buscando todas as consultas");
        return consultaRepository.findAll();
    }
}
