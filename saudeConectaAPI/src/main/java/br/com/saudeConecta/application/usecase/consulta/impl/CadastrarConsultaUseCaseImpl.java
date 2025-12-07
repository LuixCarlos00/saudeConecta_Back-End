package br.com.saudeConecta.application.usecase.consulta.impl;

import br.com.saudeConecta.application.usecase.consulta.CadastrarConsultaUseCase;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarConsultaUseCaseImpl implements CadastrarConsultaUseCase {

    private final ConsultaRepository consultaRepository;

    @Override
    public Consulta executar(Consulta consulta) {
        log.info("Cadastrando nova consulta para paciente: {}", 
                consulta.getConPaciente() != null ? consulta.getConPaciente().getPaciNome() : "N/A");
        Consulta consultaSalva = consultaRepository.save(consulta);
        log.info("Consulta cadastrada com sucesso. ID: {}", consultaSalva.getConCodigoConsulta());
        return consultaSalva;
    }
}
