package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ConsultaRepository consultaRepository;

    /**
     * Cadastra um novo prontuário médico
     * @param request Dados do prontuário a ser cadastrado
     * @return Prontuário cadastrado
     */
    @Transactional
    public Prontuario cadastrarProntuarioMedico(CadastrarProntuarioRequest request) {
        log.info("Iniciando cadastro de prontuário - Médico ID: {}, Consulta ID: {}", 
                request.prontCodigoMedico(), request.consulta());

        // Buscar profissional
        Profissional profissional = profissionalRepository.findById(request.prontCodigoMedico())
                .orElseThrow(() -> {
                    log.error("Profissional não encontrado - ID: {}", request.prontCodigoMedico());
                    return new EntityNotFoundException("Profissional não encontrado com ID: " + request.prontCodigoMedico());
                });

        // Buscar consulta
        Consulta consulta = consultaRepository.findById(request.consulta())
                .orElseThrow(() -> {
                    log.error("Consulta não encontrada - ID: {}", request.consulta());
                    return new EntityNotFoundException("Consulta não encontrada com ID: " + request.consulta());
                });

        // Verificar se já existe prontuário para esta consulta
        Prontuario prontuarioExistente = prontuarioRepository.findByConsulta_Id(request.consulta());
        if (prontuarioExistente != null) {
            log.warn("Já existe prontuário para a consulta ID: {}", request.consulta());
            throw new IllegalStateException("Já existe um prontuário cadastrado para esta consulta");
        }

        // Criar novo prontuário
        Prontuario prontuario = new Prontuario(request, profissional, consulta);
        Prontuario prontuarioSalvo = prontuarioRepository.save(prontuario);

        // Atualizar status da consulta para REALIZADA
        consulta.setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(consulta);
        log.info("Status da consulta ID: {} atualizado para REALIZADA", consulta.getId());

        log.info("Prontuário cadastrado com sucesso - ID: {}", prontuarioSalvo.getProntCodigoProntuario());
        return prontuarioSalvo;
    }

    /**
     * Busca prontuário por ID da consulta
     * @param consultaId ID da consulta
     * @return Prontuário encontrado
     */
    @Transactional(readOnly = true)
    public Prontuario buscarProntuarioById(Long consultaId) {
        log.debug("Buscando prontuário por consulta ID: {}", consultaId);
        Prontuario prontuario = prontuarioRepository.findByConsulta_Id(consultaId);
        
        if (prontuario == null) {
            log.warn("Prontuário não encontrado para consulta ID: {}", consultaId);
            throw new EntityNotFoundException("Prontuário não encontrado para a consulta ID: " + consultaId);
        }
        
        return prontuario;
    }

    /**
     * Busca todos os prontuários de um paciente
     * @param pacienteId ID do paciente
     * @return Lista de prontuários do paciente
     */
    @Transactional(readOnly = true)
    public List<Prontuario> buscarPorPaciente(Long pacienteId) {
        log.debug("Buscando prontuários do paciente ID: {}", pacienteId);
        return prontuarioRepository.findByConsulta_Paciente_PaciCodigo(pacienteId);
    }

    /**
     * Busca todos os prontuários de um profissional
     * @param profissionalId ID do profissional
     * @return Lista de prontuários do profissional
     */
    @Transactional(readOnly = true)
    public List<Prontuario> buscarPorProfissional(Long profissionalId) {
        log.debug("Buscando prontuários do profissional ID: {}", profissionalId);
        return prontuarioRepository.findByProfissional_Id(profissionalId);
    }

    /**
     * Busca prontuário por ID
     * @param id ID do prontuário
     * @return Prontuário encontrado
     */
    @Transactional(readOnly = true)
    public Prontuario buscarPorId(Long id) {
        log.debug("Buscando prontuário por ID: {}", id);
        return prontuarioRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prontuário não encontrado - ID: {}", id);
                    return new EntityNotFoundException("Prontuário não encontrado com ID: " + id);
                });
    }
}
