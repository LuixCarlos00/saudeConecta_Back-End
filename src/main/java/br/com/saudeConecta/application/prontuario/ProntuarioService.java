package br.com.saudeConecta.application.prontuario;

import br.com.saudeConecta.domain.FusoHorario;
import org.springframework.http.HttpStatus;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioRequest;
import br.com.saudeConecta.application.prontuario.dto.PlanejamentoTerapeuticoRequest;
import br.com.saudeConecta.application.prontuario.mapper.ProntuarioMapper;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AtendimentoRascunhoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ConsultaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.PacienteRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProntuarioRepository;
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

    /** Código do 409 quando a consulta já tem prontuário (ver docs do ErrorResponse). */
    public static final String CODE_PRONTUARIO_JA_FINALIZADO = "PRONTUARIO_JA_FINALIZADO";

    private final ProntuarioRepository prontuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ConsultaRepository consultaRepository;
    private final AtendimentoRascunhoRepository atendimentoRascunhoRepository;
    private final PacienteRepository pacienteRepository;
    private final PlanejamentoTerapeuticoService planejamentoService;
    private final TenantHelper tenantHelper;
    private final CacheEvictionService cacheEvictionService;

    // Chave de cache sempre prefixada pela organização do token (mesmo formato usado na evicção).
    private static final String CHAVE_CACHE_CONSULTA_TENANT =
            "T(br.com.saudeConecta.application.cache.CacheEvictionService).chaveConsultaPorTenant("
                    + "@tenantHelper.getCurrentTenantIdOrNull(), #consultaId)";

    /**
     * Cadastra um novo prontuario Medico com planejamentos terapêuticos
     * @param request Dados do prontuario a ser cadastrado
     * @return prontuario cadastrado
     */
    @Transactional
    public Prontuario cadastrarProntuarioMedico(CadastrarProntuarioRequest request) {
        log.info("prontuario.medico.cadastrar inicio consultaId={} profissionalId={}", request.getConsulta(), request.getCodigoMedico());
        Long orgId = tenantHelper.getCurrentTenantId();

        // Profissional e consulta precisam ser da clínica do token; de outra clínica = "não encontrado".
        Profissional profissional = profissionalRepository
                .findByIdAndOrganizacao_Id(request.getCodigoMedico(), orgId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profissional nao encontrado: " + request.getCodigoMedico()));

        // Lock da consulta antes de qualquer INSERT: mesma ordem de locks do PUT do rascunho (sem deadlock
        // pela FK do prontuario) e duplo clique em "Finalizar" fica serializado. Tenant continua no filtro.
        Consulta consulta = consultaRepository
                .buscarComLock(request.getConsulta(), orgId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Consulta nao encontrada: " + request.getConsulta()));

        // Regra: um prontuário por consulta. Com o lock acima, o segundo "Finalizar" (duplo clique,
        // duas abas) espera o primeiro terminar e cai aqui em vez de gravar um prontuário duplicado.
        if (prontuarioRepository.existsByConsulta_Id(consulta.getId())) {
            log.warn("prontuario.medico.cadastrar negado motivo=PRONTUARIO_JA_FINALIZADO consultaId={}", consulta.getId());
            throw new BusinessException(
                    "Esta consulta já foi finalizada. Para corrigir o prontuário, use a edição.",
                    HttpStatus.CONFLICT, CODE_PRONTUARIO_JA_FINALIZADO);
        }

        Prontuario prontuario = ProntuarioMapper.paraEntidade(request, profissional, consulta);

        Prontuario salvo = prontuarioRepository.save(prontuario);
        log.info("prontuario.medico.cadastrar ok prontuarioId={}", salvo.getProntCodigoProntuario());

        // ── Adiciona planejamentos terapêuticos ─────────────────────────────────
        if (!CollectionUtils.isEmpty(request.getPlanejamentos())) {
            log.debug("prontuario.medico.cadastrar planejamentos quantidade={}", request.getPlanejamentos().size());
            Organizacao organizacao = new Organizacao();
            organizacao.setId(orgId);

            for (CadastrarProntuarioRequest.PlanejamentoItem item : request.getPlanejamentos()) {
                log.debug("prontuario.medico.cadastrar planejamento valor={} pacienteId={}",
                        item.getValor(), item.getPacienteId());
                
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
                log.debug("prontuario.medico.cadastrar planejamento adicionado");
            }
            prontuarioRepository.save(salvo);
            log.debug("prontuario.medico.cadastrar salvo planejamentos={}", salvo.getPlanejamentos().size());
            
            // Verificação adicional
            salvo.getPlanejamentos().forEach(p -> {
                log.debug("prontuario.medico.cadastrar planejamento salvo planejamentoId={} valor={}",
                        p.getId(), p.getValor());
            });
        } else {
            log.debug("prontuario.medico.cadastrar planejamentos quantidade=0");
        }

        consulta.setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(consulta);
        log.info("prontuario.medico.cadastrar consulta consultaId={} status=REALIZADA", consulta.getId());

        // Prontuário oficial gravado: o rascunho sai na mesma transação (falhou acima = rascunho continua).
        int rascunhos = atendimentoRascunhoRepository.apagarPorConsulta(consulta.getId());
        log.info("prontuario.medico.cadastrar rascunho consultaId={} removidos={}", consulta.getId(), rascunhos);
        cacheEvictionService.evictProntuarioRecente(orgId, consulta.getId());

        return salvo;
    }

    @Transactional
    public void atualizarProntuarioMedico(Long id, CadastrarProntuarioRequest request) {
        log.info("prontuario.medico.atualizar inicio prontuarioId={}", id);
        Long orgId = tenantHelper.getCurrentTenantId();

        // Dado clínico é do profissional que atendeu: nem o gestor edita. Outro usuário da clínica recebe
        // o mesmo 404 de prontuário inexistente (padrão IDOR do rascunho: não revela que ele existe).
        Prontuario prontuario = prontuarioRepository.findByProntCodigoProntuarioAndConsulta_Organizacao_Id(id, orgId)
                .filter(p -> ehDonoDaConsulta(p.getConsulta()))
                .orElseThrow(() -> {
                    log.warn("prontuario.medico.atualizar negado motivo=NAO_ENCONTRADO prontuarioId={}", id);
                    return new ResourceNotFoundException("Prontuário não encontrado: " + id);
                });
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
        // prontDataFinalizado não muda na edição: é a data em que o atendimento foi finalizado

        // Planejamentos: atualiza por id e preserva os assinados (ver EdicaoPlanejamentos). O paciente é
        // sempre o da consulta: o pacienteId do payload não troca o paciente do planejamento.
        Organizacao organizacao = new Organizacao();
        organizacao.setId(orgId);
        Consulta consulta = prontuario.getConsulta();
        Profissional profissional = prontuario.getProfissional();
        List<EdicaoPlanejamentos.Item> itens = request.getPlanejamentos() == null ? null
                : request.getPlanejamentos().stream()
                        .map(i -> new EdicaoPlanejamentos.Item(i.getId(),
                                parseDataToLocalDate(parseData(i.getDataProcedimento())),
                                i.getProcedimentoRealizado(), i.getValor()))
                        .toList();
        EdicaoPlanejamentos.sincronizar(prontuario.getPlanejamentos(), itens,
                item -> PlanejamentoTerapeutico.builder()
                        .prontuario(prontuario)
                        .consulta(consulta)
                        .paciente(consulta.getPaciente())
                        .profissional(profissional)
                        .organizacao(organizacao)
                        .dataProcedimento(item.dataProcedimento())
                        .procedimentoRealizado(item.procedimentoRealizado())
                        .valor(item.valor())
                        .statusAssinatura("PENDENTE")
                        .build(),
                (gravado, item) -> {
                    gravado.setDataProcedimento(item.dataProcedimento());
                    gravado.setProcedimentoRealizado(item.procedimentoRealizado());
                    gravado.setValor(item.valor());
                },
                id);

        prontuarioRepository.save(prontuario);
        log.info("prontuario.medico.atualizar ok prontuarioId={}", id);
    }

    /**
     * Indica se o usuário do token é o profissional da consulta ({@code consulta.profissional.usuario}).
     * Consulta, profissional e tenant do prontuário nunca vêm do payload da edição.
     */
    private boolean ehDonoDaConsulta(Consulta consulta) {
        Long usuarioId = tenantHelper.getCurrentUserId();
        return usuarioId != null && consulta != null && consulta.getProfissional() != null
                && consulta.getProfissional().getUsuario() != null
                && usuarioId.equals(consulta.getProfissional().getUsuario().getId());
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
        log.debug("prontuario.medico.buscar consultaId={}", consultaId);
        Long orgId = tenantHelper.getCurrentTenantId();
        Prontuario prontuario = prontuarioRepository.findByConsultaIdAndOrganizacaoIdWithFetch(consultaId, orgId);
        
        if (prontuario == null) {
            log.warn("prontuario.medico.buscar negado motivo=NAO_ENCONTRADO consultaId={}", consultaId);
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
        log.debug("prontuario.medico.buscarRecente consultaId={}", consultaId);
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

    private Date parseData(Date data) {
        return data;
    }

    private LocalDate parseDataToLocalDate(Date data) {
        if (data == null) return LocalDate.now(FusoHorario.ZONA);
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
