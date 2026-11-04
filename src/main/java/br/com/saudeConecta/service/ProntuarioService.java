package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioRequest;
import br.com.saudeConecta.presentation.dto.prontuario.PlanejamentoTerapeuticoRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final TenantHelper tenantHelper;
    private final CacheEvictionService cacheEvictionService;

    // Chave de cache sempre prefixada pela organização do token (mesmo formato usado na evicção).
    private static final String CHAVE_CACHE_CONSULTA_TENANT =
            "T(br.com.saudeConecta.service.CacheEvictionService).chaveConsultaPorTenant("
                    + "@tenantHelper.getCurrentTenantIdOrNull(), #consultaId)";

    /**
     * Cadastra um novo prontuario Medico com planejamentos terapêuticos
     * @param request Dados do prontuario a ser cadastrado
     * @return prontuario cadastrado
     */
    @Transactional
    public Prontuario cadastrarProntuarioMedico(CadastrarProntuarioRequest request) {
        log.info("Cadastrando prontuario médico — consulta={}", request.getConsulta());
        Long orgId = tenantHelper.getCurrentTenantId();

        // Profissional e consulta precisam ser da clínica do token; de outra clínica = "não encontrado".
        Profissional profissional = profissionalRepository
                .findByIdAndOrganizacao_Id(request.getCodigoMedico(), orgId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profissional nao encontrado: " + request.getCodigoMedico()));

        Consulta consulta = consultaRepository
                .findByIdAndOrganizacao_Id(request.getConsulta(), orgId)
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
                .prontOrientacoes(request.getOrientacoes())
                // prescrição
                 .prontTituloPrescricao(request.getTituloPrescricao())
                .prontPrescricao(request.getPrescricao())
                // exames
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
            Organizacao organizacao = new Organizacao();
            organizacao.setId(orgId);

            for (CadastrarProntuarioRequest.PlanejamentoItem item : request.getPlanejamentos()) {
                log.info("Processando planejamento: procedimento={}, valor={}, pacienteId={}", 
                    item.getProcedimentoRealizado(), item.getValor(), item.getPacienteId());
                
                Paciente paciente = null;
                if (item.getPacienteId() != null) {
                    paciente = pacienteRepository
                            .findByPaciCodigoAndOrganizacao_IdWithEndereco(item.getPacienteId(), orgId)
                            .orElse(null);
                }

                PlanejamentoTerapeutico planejamento = PlanejamentoTerapeutico.builder()
                        .prontuario(salvo)  // Vincula ao prontuario médico
                        .consulta(consulta)
                        .paciente(paciente)
                        .profissional(profissional)
                        .organizacao(organizacao)
                        .dataProcedimento(parseDataToLocalDate(parseData(item.getDataProcedimento())))
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
        cacheEvictionService.evictProntuarioRecente(orgId, consulta.getId());

        return salvo;
    }

    @Transactional
    public void atualizarProntuarioMedico(Long id, CadastrarProntuarioRequest request) {
        log.info("Atualizando prontuário médico — id={}", id);
        Long orgId = tenantHelper.getCurrentTenantId();

        Prontuario prontuario = prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prontuário não encontrado: " + id));
        cacheEvictionService.evictProntuarioRecente(orgId, prontuario.getConsulta().getId());

        prontuario.setProntPeso(request.getPeso());
        prontuario.setProntAltura(request.getAltura());
        prontuario.setProntTemperatura(request.getTemperatura());
        prontuario.setProntSaturacao(request.getSaturacao());
        prontuario.setProntHemoglobina(request.getHemoglobina());
        prontuario.setProntPressao(request.getPressao());
        prontuario.setProntFrequenciaRespiratoria(request.getFrequenciaRespiratoria());
        prontuario.setProntFrequenciaArterialSistolica(request.getFrequenciaArterialSistolica());
        prontuario.setProntFrequenciaArterialDiastolica(request.getFrequenciaArterialDiastolica());
        prontuario.setProntPulso(request.getPulso());
        prontuario.setProntObservacao(request.getObservacao());
        prontuario.setProntAnamnese(request.getAnamnese());
        prontuario.setProntQueixaPricipal(request.getQueixaPrincipal());
        prontuario.setProntDiagnostico(request.getDiagnostico());
        prontuario.setProntOrientacoes(request.getOrientacoes());
         prontuario.setProntResponsavel(request.getResponsavel());
        prontuario.setProntPrescricao(request.getPrescricao());
        prontuario.setProntTituloPrescricao(request.getTituloPrescricao());
         prontuario.setProntTussTexto(request.getTussTexto());
        prontuario.setProntCidTexto(request.getCidTexto());
        prontuario.setProntSolicitacaoExameTexto(request.getSolicitacaoExameTexto());
        prontuario.setProntDataFinalizado(request.getDataFinalizado());

        // Planejamentos: limpa e recria (mesmo padrão do dentista)
        prontuario.getPlanejamentos().clear();
        prontuarioRepository.saveAndFlush(prontuario);

        if (!CollectionUtils.isEmpty(request.getPlanejamentos())) {
            Organizacao organizacao = new Organizacao();
            organizacao.setId(orgId);

            Consulta consulta = prontuario.getConsulta();
            Profissional profissional = prontuario.getProfissional();

            for (CadastrarProntuarioRequest.PlanejamentoItem item : request.getPlanejamentos()) {
                Paciente paciente = null;
                if (item.getPacienteId() != null) {
                    paciente = pacienteRepository
                            .findByPaciCodigoAndOrganizacao_IdWithEndereco(item.getPacienteId(), orgId)
                            .orElse(null);
                }
                PlanejamentoTerapeutico p = PlanejamentoTerapeutico.builder()
                        .prontuario(prontuario)
                        .consulta(consulta)
                        .paciente(paciente)
                        .profissional(profissional)
                        .organizacao(organizacao)
                        .dataProcedimento(parseDataToLocalDate(parseData(item.getDataProcedimento())))
                        .procedimentoRealizado(item.getProcedimentoRealizado())
                        .valor(item.getValor())
                        .statusAssinatura("PENDENTE")
                        .build();
                prontuario.addPlanejamento(p);
            }
        }

        prontuarioRepository.save(prontuario);
        log.info("Prontuário médico atualizado — id={}", id);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public Prontuario buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(id, orgId)
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
        Long orgId = tenantHelper.getCurrentTenantId();
        Prontuario prontuario = prontuarioRepository.findByConsultaIdAndOrganizacaoIdWithFetch(consultaId, orgId);
        
        if (prontuario == null) {
            log.warn("prontuario nao encontrado para consulta ID: {}", consultaId);
            throw new EntityNotFoundException("prontuario nao encontrado para a consulta ID: " + consultaId);
        }
        
        return prontuario;
    }

    /**
     * Busca o prontuário médico mais recente de uma consulta.
     * Usa 2 queries para evitar cartesian product com planejamentos.
     *
     * @param consultaId ID da consulta
     * @return prontuário mais recente encontrado
     */
    @Cacheable(value = "prontuario-recente", key = CHAVE_CACHE_CONSULTA_TENANT)
    @Transactional(readOnly = true)
    public Prontuario buscarMaisRecentePorConsulta(Long consultaId) {
        log.debug("Buscando prontuário médico mais recente para consulta ID: {}", consultaId);
        Long orgId = tenantHelper.getCurrentTenantId();

        Long id = prontuarioRepository
                .findIdMaisRecentePorConsulta(consultaId, orgId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenhum prontuário médico encontrado para consulta: " + consultaId));

        return prontuarioRepository
                .findByIdWithFullFetch(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Prontuário médico não encontrado: id=" + id));
    }

    /**
     * Busca todos os prontuarios de um paciente
     * @param pacienteId ID do paciente
     * @return Lista de prontuarios do paciente
     */
    @Transactional(readOnly = true)
    public List<Prontuario> buscarPorPaciente(Long pacienteId) {
        log.debug("Buscando prontuarios do paciente ID: {}", pacienteId);
        Long orgId = tenantHelper.getCurrentTenantId();
        return prontuarioRepository.findByConsulta_Paciente_PaciCodigoAndConsulta_Organizacao_Id(pacienteId, orgId);
    }

    /**
     * Busca todos os prontuarios de um profissional
     * @param profissionalId ID do profissional
     * @return Lista de prontuarios do profissional
     */
    @Transactional(readOnly = true)
    public List<Prontuario> buscarPorProfissional(Long profissionalId) {
        log.debug("Buscando prontuarios do profissional ID: {}", profissionalId);
        Long orgId = tenantHelper.getCurrentTenantId();
        return prontuarioRepository.findByProfissional_IdAndConsulta_Organizacao_Id(profissionalId, orgId);
    }

    private Date parseData(Date data) {
        return data;
    }

    private LocalDate parseDataToLocalDate(Date data) {
        if (data == null) return LocalDate.now();
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
