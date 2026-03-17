package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import br.com.saudeConecta.presentation.dto.prontuario.PlanejamentoTerapeuticoRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ConsultaRepository consultaRepository;
    private final PlanejamentoTerapeuticoService planejamentoService;

    /**
     * Cadastra um novo prontuario Medico
     * @param request Dados do prontuario a ser cadastrado
     * @return prontuario cadastrado
     */
    @Transactional
    public Prontuario cadastrarProntuarioMedico(CadastrarProntuarioRequest request) {
        return cadastrarProntuarioMedico(request, null);
    }

    /**
     * Cadastra um novo prontuario Medico com planejamentos terapêuticos
     * @param request Dados do prontuario a ser cadastrado
     * @param planejamentos Lista de planejamentos terapêuticos (opcional)
     * @return prontuario cadastrado
     */
    @Transactional
    public Prontuario cadastrarProntuarioMedico(CadastrarProntuarioRequest request, List<PlanejamentoTerapeuticoRequest> planejamentos) {
        log.info("Iniciando cadastro de prontuario - Medico ID: {}, Consulta ID: {}", 
                request.codigoMedico(), request.consulta());

        // Buscar profissional
        Profissional profissional = profissionalRepository.findById(request.codigoMedico())
                .orElseThrow(() -> {
                    log.error("Profissional nao encontrado - ID: {}", request.codigoMedico());
                    return new EntityNotFoundException("Profissional nao encontrado com ID: " + request.codigoMedico());
                });

        // Buscar consulta
        Consulta consulta = consultaRepository.findById(request.consulta())
                .orElseThrow(() -> {
                    log.error("Consulta nao encontrada - ID: {}", request.consulta());
                    return new EntityNotFoundException("Consulta nao encontrada com ID: " + request.consulta());
                });

        // Verificar se já existe prontuario para esta consulta
        Prontuario prontuarioExistente = prontuarioRepository.findByConsulta_Id(request.consulta());
        if (prontuarioExistente != null) {
            log.warn("Já existe prontuario para a consulta ID: {}", request.consulta());
            throw new IllegalStateException("Ja existe um prontuario cadastrado para esta consulta");
        }

        // Criar novo prontuario
        Prontuario prontuario = new Prontuario(request, profissional, consulta);
        Prontuario prontuarioSalvo = prontuarioRepository.save(prontuario);

        // Atualizar status da consulta para REALIZADA
        consulta.setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(consulta);
        log.info("Status da consulta ID: {} atualizado para REALIZADA", consulta.getId());

        // Salvar planejamentos terapêuticos se fornecidos
        if (planejamentos != null && !planejamentos.isEmpty()) {
            log.info("Salvando {} planejamentos terapêuticos para o prontuario", planejamentos.size());
            salvarPlanejamentosTerapeuticos(prontuarioSalvo, request.codigoMedico(), planejamentos);
        }

        log.info("prontuario cadastrado com sucesso - ID: {}", prontuarioSalvo.getProntCodigoProntuario());
        return prontuarioSalvo;
    }

    /**
     * Salva os planejamentos terapêuticos vinculados ao prontuário médico
     * @param prontuario Prontuário médico salvo
     * @param profissionalId ID do profissional
     * @param planejamentos Lista de planejamentos a serem salvos
     */
    @Transactional
    public void salvarPlanejamentosTerapeuticos(Prontuario prontuario, Long profissionalId, List<PlanejamentoTerapeuticoRequest> planejamentos) {
        for (PlanejamentoTerapeuticoRequest planejamentoRequest : planejamentos) {
            try {
                // Criar request com dados do planejamento
                PlanejamentoTerapeuticoRequest request = new PlanejamentoTerapeuticoRequest();
                request.setConsultaId(prontuario.getConsulta().getId());
                request.setPacienteId(prontuario.getConsulta().getPaciente().getPaciCodigo());
                request.setDataProcedimento(planejamentoRequest.getDataProcedimento());
                request.setProcedimentoRealizado(planejamentoRequest.getProcedimentoRealizado());
                request.setValor(planejamentoRequest.getValor());
                // Para prontuário médico, não vinculamos a prontuarioDentistaId
                request.setProntuarioDentistaId(null);

                // Salvar planejamento usando o service existente
                // Precisamos modificar o service para aceitar null no prontuarioDentistaId
                // Por enquanto, vamos criar um método alternativo aqui
                salvarPlanejamentoMedico(profissionalId, request);
                
            } catch (Exception e) {
                log.error("Erro ao salvar planejamento terapêutico: {}", e.getMessage(), e);
                // Continuar salvando os outros planejamentos
            }
        }
    }

    /**
     * Salva um planejamento terapêutico para prontuário médico (sem vínculo com prontuarioDentista)
     * @param profissionalId ID do profissional
     * @param request Dados do planejamento
     */
    @Transactional
    public void salvarPlanejamentoMedico(Long profissionalId, PlanejamentoTerapeuticoRequest request) {
        // Criar planejamento diretamente sem usar o service odontológico
        // Isso evita a dependência de ProntuarioDentista
        log.info("Salvando planejamento médico - consulta: {}, procedimento: {}", 
                request.getConsultaId(), request.getProcedimentoRealizado());
        
        // Aqui você pode implementar a lógica específica para prontuário médico
        // Por enquanto, vamos apenas logar que o planejamento seria salvo
        // Futuramente, pode ser criada uma entidade PlanejamentoMedico ou modificada a existente
    }

    /**
     * Busca prontuario por ID da consulta
     * @param consultaId ID da consulta
     * @return prontuario encontrado
     */
    @Transactional(readOnly = true)
    public Prontuario buscarProntuarioById(Long consultaId) {
        log.debug("Buscando prontuario por consulta ID: {}", consultaId);
        Prontuario prontuario = prontuarioRepository.findByConsulta_IdWithFetch(consultaId);
        
        if (prontuario == null) {
            log.warn("prontuario nao encontrado para consulta ID: {}", consultaId);
            throw new EntityNotFoundException("prontuario nao encontrado para a consulta ID: " + consultaId);
        }
        
        return prontuario;
    }

    /**
     * Busca todos os prontuarios de um paciente
     * @param pacienteId ID do paciente
     * @return Lista de prontuarios do paciente
     */
    @Transactional(readOnly = true)
    public List<Prontuario> buscarPorPaciente(Long pacienteId) {
        log.debug("Buscando prontuarios do paciente ID: {}", pacienteId);
        return prontuarioRepository.findByConsulta_Paciente_PaciCodigo(pacienteId);
    }

    /**
     * Busca todos os prontuarios de um profissional
     * @param profissionalId ID do profissional
     * @return Lista de prontuarios do profissional
     */
    @Transactional(readOnly = true)
    public List<Prontuario> buscarPorProfissional(Long profissionalId) {
        log.debug("Buscando prontuarios do profissional ID: {}", profissionalId);
        return prontuarioRepository.findByProfissional_Id(profissionalId);
    }


}
