package br.com.saudeConecta.application.usecase.medico.impl;

import br.com.saudeConecta.application.usecase.medico.CadastrarMedicoUseCase;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.infrastructure.persistence.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarMedicoUseCaseImpl implements CadastrarMedicoUseCase {

    private final MedicoRepository medicoRepository;

    @Override
    public Medico executar(Medico medico) {
        log.info("Cadastrando novo medico: {}", medico.getMedNome());
        Medico medicoSalvo = medicoRepository.save(medico);
        log.info("Medico cadastrado com sucesso. ID: {}", medicoSalvo.getMedCodigo());
        return medicoSalvo;
    }
}
