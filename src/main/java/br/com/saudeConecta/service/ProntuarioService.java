package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import br.com.saudeConecta.presentation.dto.prontuario.PlanejamentoTerapeuticoRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProntuarioService {

    private final ProntuarioRepository prontuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final PlanejamentoTerapeuticoService planejamentoService;

    /**
     * Cadastra um novo prontuario Medico com planejamentos terapêuticos
     * @param request Dados do prontuario a ser cadastrado
     * @return prontuario cadastrado
     */
    @Transactional
    public Prontuario cadastrarProntuarioMedico(CadastrarProntuarioRequest request) {
        log.info("Cadastrando prontuario médico — consulta={}", request.getConsulta());

        Profissional profissional = profissionalRepository
                .findById(request.getCodigoMedico())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profissional nao encontrado: " + request.getCodigoMedico()));

        Consulta consulta = consultaRepository
                .findById(request.getConsulta())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Consulta nao encontrada: " + request.getConsulta()));

        // ── Monta Prontuario principal ────────────────────────────────────────
        Prontuario prontuario = Prontuario.builder()
                // anamnese
                .prontQueixaPricipal(request.getQueixaPrincipal())
                .prontAnamnese(request.getAnamnese())
                .prontObservacao(request.getObservacao())
                // exame clínico — sinais vitais
                .prontPressao(request.getPressao())
                .prontFrequenciaRespiratoria(request.getFrequenciaRespiratoria())
                .prontFrequenciaArterialSistolica(request.getFrequenciaArterialSistolica())
                .prontFrequenciaArterialDiastolica(request.getFrequenciaArterialDiastolica())
                .prontPulso(request.getPulso())
                .prontAltura(request.getAltura())
                .prontTemperatura(request.getTemperatura())
                .prontPeso(request.getPeso())
                .prontSaturacao(request.getSaturacao())
                .prontHemoglobina(request.getHemoglobina())
                // diagnóstico
                .prontDiagnostico(request.getDiagnostico())
                // prescrição
                .prontModeloPrescricao(request.getModeloPrescricao())
                .prontTituloPrescricao(request.getTituloPrescricao())
                .prontDataPrescricao(request.getDataPrescricao())
                .prontPrescricao(request.getPrescricao())
                // exames
                .prontExameOutros(request.getExame())
                // controle
                .prontDataFinalizado(request.getDataFinalizado())
                .prontTempoDuracao(request.getTempoDuracao())
                // identificação do paciente
                .prontResponsavel(request.getResponsavel())
                // TUSS e CID
                .prontTussTexto(request.getTussTexto())
                .prontCidTexto(request.getCidTexto())
                .prontSolicitacaoExameTexto(request.getSolicitacaoExameTexto())
                // relacionamentos
                .profissional(profissional)
                .consulta(consulta)
                .build();

        Prontuario salvo = prontuarioRepository.save(prontuario);
        log.info("Prontuario médico salvo — id={}", salvo.getProntCodigoProntuario());

        // ── Adiciona planejamentos terapêuticos ─────────────────────────────────
        if (!CollectionUtils.isEmpty(request.getPlanejamentos())) {
            log.info("Processando {} planejamentos", request.getPlanejamentos().size());
            Long orgId = TenantContext.getCurrentTenant();
            Organizacao organizacao = new Organizacao();
            organizacao.setId(orgId);

            for (CadastrarProntuarioRequest.PlanejamentoItem item : request.getPlanejamentos()) {
                log.info("Processando planejamento: procedimento={}, valor={}, pacienteId={}", 
                    item.getProcedimentoRealizado(), item.getValor(), item.getPacienteId());
                
                Paciente paciente = null;
                if (item.getPacienteId() != null) {
                    paciente = pacienteRepository.findById(item.getPacienteId()).orElse(null);
                }

                PlanejamentoTerapeutico planejamento = PlanejamentoTerapeutico.builder()
                        .prontuario(salvo)  // Vincula ao prontuario médico
                        .consulta(consulta)
                        .paciente(paciente)
                        .profissional(profissional)
                        .organizacao(organizacao)
                        .dataProcedimento(parseData(item.getDataProcedimento()).toLocalDate())
                        .procedimentoRealizado(item.getProcedimentoRealizado())
                        .valor(item.getValor())
                        .statusAssinatura("PENDENTE")
                        .build();
                salvo.addPlanejamento(planejamento);
                log.info("Planejamento adicionado ao prontuario");
            }
            prontuarioRepository.save(salvo);
            log.info("Prontuario salvo com {} planejamentos", salvo.getPlanejamentos().size());
            
            // Verificação adicional
            salvo.getPlanejamentos().forEach(p -> {
                log.info("Planejamento salvo: ID={}, Procedimento={}, Valor={}", 
                    p.getId(), p.getProcedimentoRealizado(), p.getValor());
            });
        } else {
            log.info("Nenhum planejamento para processar");
        }

        consulta.setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(consulta);
        log.info("Status da consulta ID: {} atualizado para REALIZADA", consulta.getId());

        return salvo;
    }

    /**
     * Atualiza um Prontuario médico existente.
     *
     * @param id      ID do Prontuario a ser atualizado
     * @param request Dados atualizados do Prontuario
     */
    @Transactional
    public void atualizarProntuarioMedico(Long id, CadastrarProntuarioRequest request) {
        log.info("Atualizando prontuario médico — id={}", id);

        Prontuario prontuario = prontuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuario médico nao encontrado: " + id));

        // ── Anamnese ──
        prontuario.setProntQueixaPricipal(request.getQueixaPrincipal());
        prontuario.setProntAnamnese(request.getAnamnese());
        prontuario.setProntObservacao(request.getObservacao());

        // ── Sinais Vitais ──
        prontuario.setProntPressao(request.getPressao());
        prontuario.setProntFrequenciaRespiratoria(request.getFrequenciaRespiratoria());
        prontuario.setProntFrequenciaArterialSistolica(request.getFrequenciaArterialSistolica());
        prontuario.setProntFrequenciaArterialDiastolica(request.getFrequenciaArterialDiastolica());
        prontuario.setProntPulso(request.getPulso());
        prontuario.setProntAltura(request.getAltura());
        prontuario.setProntTemperatura(request.getTemperatura());
        prontuario.setProntPeso(request.getPeso());
        prontuario.setProntSaturacao(request.getSaturacao());
        prontuario.setProntHemoglobina(request.getHemoglobina());

        // ── Diagnóstico ──
        prontuario.setProntDiagnostico(request.getDiagnostico());

        // ── Prescrição ──
        prontuario.setProntModeloPrescricao(request.getModeloPrescricao());
        prontuario.setProntTituloPrescricao(request.getTituloPrescricao());
        prontuario.setProntDataPrescricao(request.getDataPrescricao());
        prontuario.setProntPrescricao(request.getPrescricao());

        // ── Exames ──
        prontuario.setProntExameOutros(request.getExame());

        // ── TUSS e CID ──
        prontuario.setProntTussTexto(request.getTussTexto());
        prontuario.setProntCidTexto(request.getCidTexto());
        prontuario.setProntSolicitacaoExameTexto(request.getSolicitacaoExameTexto());

        // ── Identificação ──
        prontuario.setProntResponsavel(request.getResponsavel());

        // ── Controle ──
        prontuario.setProntDataFinalizado(request.getDataFinalizado());
        prontuario.setProntTempoDuracao(request.getTempoDuracao());

        // ── Atualiza planejamentos terapêuticos (limpa e recria) ──
        prontuario.getPlanejamentos().clear();
        prontuarioRepository.saveAndFlush(prontuario);

        // Note: Planejamentos não são atualizados neste método para manter consistência
        // com o padrão do dentista onde planejamentos são tratados separadamente

        prontuarioRepository.save(prontuario);
        log.info("Prontuario médico atualizado — id={}", id);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public Prontuario buscarPorId(Long id) {
        return prontuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuario médico nao encontrado: " + id));
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

    private Date parseData(Date data) {
        return data;
    }

    private LocalDate parseDataToLocalDate(Date data) {
        if (data == null) return null;
        return data.toLocalDate();
    }

    private Date parseData(String data) {
        if (data == null || data.isBlank()) return null;
        try {
            return Date.valueOf(data);
        } catch (Exception e) {
            return null;
        }
    }


}
