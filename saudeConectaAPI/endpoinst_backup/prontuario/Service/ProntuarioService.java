package br.com.saudeConecta.endpoinst.prontuario.Service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.endpoinst.prontuario.DTO.HistoricoPaciente;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepository;
    private final ConsultaRepository consultaRepository;

    public void CadastraProntuario(Prontuario prontuario) {
        prontuarioRepository.save(prontuario);
    }

    public Optional<Prontuario> BuscaProntuario(Long idConsulta) {
        return Optional.ofNullable(prontuarioRepository.findByConsulta_ConCodigoConsulta(idConsulta));
    }

    /**
     * Busca o histórico completo de um paciente, incluindo prontuários e consultas.
     * 
     * @param idPaciente ID do paciente
     * @return HistoricoPaciente contendo prontuários e consultas do paciente
     */
    public HistoricoPaciente BuscandoHistoricoDoPaciente(Long idPaciente) {
        if (idPaciente == null) {
            log.warn("ID do paciente é nulo");
            return new HistoricoPaciente(Collections.emptyList(), Collections.emptyList());
        }

        // Busca todas as consultas do paciente
        List<Consulta> consultasDoPaciente = consultaRepository.findByConPaciente_PaciCodigo(idPaciente);
        
        if (consultasDoPaciente.isEmpty()) {
            log.info("Nenhuma consulta encontrada para o paciente ID: {}", idPaciente);
            return new HistoricoPaciente(Collections.emptyList(), Collections.emptyList());
        }

        // Busca os prontuários associados às consultas do paciente
        List<Long> idsConsultas = consultasDoPaciente.stream()
                .map(Consulta::getConCodigoConsulta)
                .collect(Collectors.toList());

        List<Prontuario> prontuariosDoPaciente = prontuarioRepository.findAll().stream()
                .filter(p -> p.getConsulta() != null && 
                            idsConsultas.contains(p.getConsulta().getConCodigoConsulta()))
                .collect(Collectors.toList());

        log.info("Encontrados {} prontuários e {} consultas para o paciente ID: {}", 
                prontuariosDoPaciente.size(), consultasDoPaciente.size(), idPaciente);

        return new HistoricoPaciente(prontuariosDoPaciente, consultasDoPaciente);
    }
}