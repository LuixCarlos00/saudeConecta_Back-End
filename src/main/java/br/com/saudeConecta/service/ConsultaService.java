package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.common.Periodo;
import br.com.saudeConecta.domain.consulta.*;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.infrastructure.persistence.specification.ConsultaSpecification;
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.HorarioOcupadoResponse;
import br.com.saudeConecta.presentation.dto.consulta.AtualizarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.text.SimpleDateFormat;
import java.text.ParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultaService {

    /** Duração assumida quando nem a consulta nem o profissional informam (mesmo default da entidade Consulta). */
    private static final int DURACAO_PADRAO_MINUTOS = 30;
    
    private final ConsultaRepository consultaRepository;
    private final ConsultaHistoricoRepository historicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProntuarioDentistaRepository prontuarioDentistaRepository;
    private final PlanejamentoTerapeuticoRepository planejamentoTerapeuticoRepository;
    private final TermoAutorizacaoRepository termoAutorizacaoRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final TenantHelper tenantHelper;
    private final ConfiguracoesConsultaService configuracoesConsultaService;

    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Consulta> buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    /**
     * Busca a consulta da organização do token já com profissional, paciente, especialidade e
     * forma de pagamento carregados, para montar o {@code ConsultaResponse} fora da transação
     * (open-in-view desligado). Sem o fetch, o proxy do profissional estoura LazyInitializationException.
     *
     * @param id ID da consulta
     * @return a consulta, ou vazio se não existir ou for de outra clínica
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Consulta> buscarPorIdComRelacoes(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_IdWithRelations(id, orgId);
    }
    
    /**
     * Busca as consultas do dia informado (ou do dia atual, se `data` for nulo).
     *
     * @param data Data de referência (opcional). Quando nula, usa a data atual do servidor.
     * @return Lista de consultas do dia de referência
     */
    @RequiresTenant
    @Cacheable(value = "consultas-hoje-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#profissionalId != null ? #profissionalId : 'all') + '-' + (#data != null ? #data : java.time.LocalDate.now())")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje(LocalDate data, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate referencia = data != null ? data : LocalDate.now();
        Periodo dia = Periodo.dia(referencia);
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, dia.inicio(), dia.fim());
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje(LocalDate data, br.com.saudeConecta.domain.usuario.Usuario usuario) {
        Long profissionalId = extrairProfissionalId(usuario);
        return buscarConsultasHoje(data, profissionalId);
    }

    @Cacheable(value = "consultas-hoje-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#data != null ? #data : java.time.LocalDate.now())")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje(LocalDate data) {
        return buscarConsultasHoje(data, (Long) null);
    }

    /**
     * Busca as consultas da semana que contém a data informada (ou a semana atual, se `data` for nulo).
     *
     * @param data Data de referência (opcional). Quando nula, usa a data atual do servidor.
     * @return Lista de consultas da semana de referência
     */
    @RequiresTenant
    @Cacheable(value = "consultas-semana-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#profissionalId != null ? #profissionalId : 'all') + '-' + (#data != null ? #data : java.time.LocalDate.now())")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual(LocalDate data, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate referencia = data != null ? data : LocalDate.now();
        Periodo semana = Periodo.semanaDe(referencia);
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, semana.inicio(), semana.fim());
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual(LocalDate data, br.com.saudeConecta.domain.usuario.Usuario usuario) {
        Long profissionalId = extrairProfissionalId(usuario);
        return buscarConsultasDaSemanaAtual(data, profissionalId);
    }

    @RequiresTenant
    @Cacheable(value = "consultas-semana-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#data != null ? #data : java.time.LocalDate.now())")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual(LocalDate data) {
        return buscarConsultasDaSemanaAtual(data, (Long) null);
    }

    /**
     * Busca as consultas do mês que contém a data informada (ou o mês atual, se `data` for nulo).
     *
     * @param data Data de referência (opcional). Quando nula, usa a data atual do servidor.
     * @return Lista de consultas do mês de referência
     */
    @RequiresTenant
    @Cacheable(value = "consultas-mes-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#profissionalId != null ? #profissionalId : 'all') + '-' + (#data != null ? #data : java.time.LocalDate.now())")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual(LocalDate data, Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate referencia = data != null ? data : LocalDate.now();
        Periodo mes = Periodo.mesDe(referencia);
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, mes.inicio(), mes.fim());
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual(LocalDate data, br.com.saudeConecta.domain.usuario.Usuario usuario) {
        Long profissionalId = extrairProfissionalId(usuario);
        return buscarConsultasDoMesAtual(data, profissionalId);
    }

    @RequiresTenant
    @Cacheable(value = "consultas-mes-org", key = "@tenantHelper.getCurrentTenantId() + '-' + (#data != null ? #data : java.time.LocalDate.now())")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual(LocalDate data) {
        return buscarConsultasDoMesAtual(data, (Long) null);
    }
    
    @RequiresTenant
    @Cacheable(value = "consultas-ano-org", key = "@tenantHelper.getCurrentTenantId()")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoAnoAtual() {
        Long orgId = tenantHelper.getCurrentTenantId();
        Periodo ano = Periodo.anoDe(LocalDate.now());
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, ano.inicio(), ano.fim());
    }
    
    /**
     * Busca consultas com filtros dinâmicos opcionais
     * 
     * @param profissionalId ID do profissional/médico (opcional)
     * @param especialidade Nome da especialidade (opcional)
     * @param dataInicio Data/hora inicial (opcional)
     * @param dataFim Data/hora final (opcional)
     * @param statusList Lista de status (opcional)
     * @return Lista de consultas que atendem aos filtros
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarComFiltrosDinamicos(
            Long profissionalId,
            String especialidade,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            List<StatusConsulta> statusList) {
        
        Long orgId = tenantHelper.getCurrentTenantId();
        
        log.debug("consulta.buscar filtros orgId={} profissionalId={} especialidade={} dataInicio={} dataFim={} status={}",
                orgId, profissionalId, especialidade, dataInicio, dataFim, statusList);
        
        return consultaRepository.findAll(
            ConsultaSpecification.buscarComFiltros(
                orgId,
                profissionalId,
                especialidade,
                dataInicio,
                dataFim,
                statusList
            )
        );
    }

    @RequiresTenant
    @CacheEvict(value = {"dashboard-admin-org", "dashboard-super-admin", "consultas-intervalo-org",
                          "consultas-hoje-org", "consultas-semana-org", "consultas-mes-org", "consultas-ano-org",
                          "horarios-ocupados-org", "disponibilidade-consulta"}, allEntries = true)
    @Transactional
    public Consulta cadastrarConsultaByOrg(AgendarConsultaRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("consulta.agendar inicio profissionalId={} pacienteId={} dataHora={} duracaoMin={}",
                request.profissionalId(), request.pacienteId(), request.dataHora(), request.duracaoMinutos());

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        // Lock pessimista no profissional: serializa agendamentos concorrentes dele até o fim da transação.
        profissionalRepository.buscarParaAgendamentoComLock(request.profissionalId(), orgId);

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        // Paciente de outra clínica responde como inexistente
        Paciente paciente = pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(request.pacienteId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        int duracaoMinutos = request.duracaoMinutos() != null
            ? request.duracaoMinutos()
            : (profissional.getTempoConsultaMinutos() != null ? profissional.getTempoConsultaMinutos() : DURACAO_PADRAO_MINUTOS);

        validarSemConflitoDeHorario(orgId, request.profissionalId(), request.dataHora(), duracaoMinutos, null);
        
        Especialidade especialidade = null;
        if (request.especialidadeId() != null) {
            especialidade = especialidadeRepository.findById(request.especialidadeId()).orElse(null);
        }
        
        FormaPagamento formaPagamento = null;
        if (request.formaPagamentoId() != null) {
            formaPagamento = buscarFormaPagamentoDaClinica(request.formaPagamentoId(), orgId);
        }
        
        Usuario criadoPor = null;
        Long userId = TenantContext.getCurrentUser();
        if (userId != null) {
            criadoPor = usuarioRepository.findById(userId).orElse(null);
        }

        boolean devePularParaConfirmado = configuracoesConsultaService.devePularParaConfirmado();
        StatusConsulta statusInicial = devePularParaConfirmado ? StatusConsulta.CONFIRMADA : StatusConsulta.AGENDADA;
        
        Consulta consulta = Consulta.builder()
            .organizacao(organizacao)
            .profissional(profissional)
            .paciente(paciente)
            .especialidade(especialidade)
            .dataHora(request.dataHora())
            .duracaoMinutos(duracaoMinutos)
            .observacoes(request.observacoes())
            .formaPagamento(formaPagamento)
            .valor(request.valor())
            .status(statusInicial)
            .criadoPor(criadoPor)
            .build();
        
        Consulta salva = consultaRepository.save(consulta);
        String mensagemHistorico = devePularParaConfirmado 
            ? "Consulta cadastrada com fluxo acelerado (status CONFIRMADA)" 
            : "Consulta cadastrada";
        registrarHistorico(salva, null, statusInicial, mensagemHistorico, criadoPor);
        
        log.info("consulta.agendar ok consultaId={} status={}", salva.getId(), salva.getStatus());
        return salva;
    }

    

    /**
     * Atualiza o status de uma consulta seguindo as regras de transição:
     * - AGENDADA → CONFIRMADA, CANCELADA
     * - CONFIRMADA → AGENDADA, CANCELADA
     * - REALIZADA → PAGO (AdminOrg pode marcar como pago após médico concluir)
     * 
     * IMPORTANTE: Status REALIZADA só é definido quando o profissional finaliza o prontuário (médico ou odontológico)
     *
     * @param id             ID da consulta
     * @param novoStatus     Novo status desejado
     * @param motivo         Motivo (obrigatório apenas para CANCELADA)
     * @return Consulta atualizada
     */
    @RequiresTenant
    @CacheEvict(value = {"dashboard-admin-org", "dashboard-profissional", "dashboard-super-admin", "consultas-intervalo-org",
                          "consultas-hoje-org", "consultas-semana-org", "consultas-mes-org", "consultas-ano-org",
                          "horarios-ocupados-org", "disponibilidade-consulta"}, allEntries = true)
    @Transactional
    public Consulta atualizarStatus(Long id, StatusConsulta novoStatus, String motivo) {
        log.info("consulta.status inicio consultaId={} novoStatus={}", id, novoStatus);
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada com ID: " + id));

        StatusConsulta statusAtual = consulta.getStatus();
        
        validarTransicaoStatus(statusAtual, novoStatus);

        if (StatusConsulta.CANCELADA.equals(novoStatus) &&
            (motivo == null || motivo.isBlank())) {
            throw new IllegalArgumentException("Motivo é obrigatório ao cancelar uma consulta");
        }

        consulta.setStatus(novoStatus);

        if (StatusConsulta.CANCELADA.equals(novoStatus)) {
            consulta.setMotivoCancelamento(motivo);
        }

        Consulta salva = consultaRepository.save(consulta);

        String descricao = gerarDescricaoHistorico(novoStatus, motivo);
        registrarHistorico(salva, statusAtual, novoStatus, descricao, getUsuarioAtual());

        log.info("consulta.status ok consultaId={} de={} para={}", salva.getId(), statusAtual, novoStatus);
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_IdWithRelations(salva.getId(), orgId)
            .orElse(salva);
    }

    /**
     * Converte o texto do status para o enum; valor desconhecido vira IllegalArgumentException (400) com mensagem clara.
     *
     * @param texto status em texto (case-insensitive)
     * @return status correspondente
     */
    private StatusConsulta parseStatus(String texto) {
        try {
            return StatusConsulta.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + texto);
        }
    }

    /**
     * Garante que o intervalo [inicio, inicio + duracao) não sobrepõe outra consulta AGENDADA/CONFIRMADA
     * do mesmo profissional (inicioExistente < fimNovo E fimExistente > inicioNovo). Consultas encostadas
     * (fim == início) não conflitam. Deve rodar com o lock do profissional já adquirido.
     *
     * @param orgId          organização
     * @param profissionalId profissional
     * @param inicio         início da nova consulta
     * @param duracaoMinutos duração da nova consulta em minutos
     * @param ignorarId      consulta a ignorar (a própria, no reagendamento); null no cadastro
     * @throws IllegalStateException se houver sobreposição
     */
    private void validarSemConflitoDeHorario(Long orgId, Long profissionalId, LocalDateTime inicio,
                                             int duracaoMinutos, Long ignorarId) {
        if (existeConflitoDeHorario(orgId, profissionalId, inicio, duracaoMinutos, ignorarId)) {
            log.warn("consulta.agendar negado motivo=HORARIO_OCUPADO profissionalId={} dataHora={}", profissionalId, inicio);
            throw new IllegalStateException("Já existe consulta agendada para este horário");
        }
    }

    /**
     * Regra única de sobreposição (usada no cadastro, no reagendamento e na verificação de disponibilidade).
     *
     * @return true quando [inicio, inicio + duracao) sobrepõe outra consulta AGENDADA/CONFIRMADA do profissional
     */
    private boolean existeConflitoDeHorario(Long orgId, Long profissionalId, LocalDateTime inicio,
                                            int duracaoMinutos, Long ignorarId) {
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
        // Janela de busca recua 1 dia para pegar consulta iniciada antes que atravesse o início novo.
        return consultaRepository
            .findAtivasDoProfissionalNoPeriodo(orgId, profissionalId, inicio.minusDays(1), fim)
            .stream()
            .filter(c -> ignorarId == null || !ignorarId.equals(c.getId()))
            .anyMatch(c -> c.getDataHora().isBefore(fim) && c.getDataHoraFim().isAfter(inicio));
    }

    /**
     * Valida se a transição de status é permitida conforme regras de negócio.
     * 
     * IMPORTANTE: Status REALIZADA não pode ser definido manualmente, apenas ao finalizar o prontuário
     */
    private void validarTransicaoStatus(StatusConsulta statusAtual, StatusConsulta novoStatus) {
        switch (statusAtual) {
            case AGENDADA:
                if (!StatusConsulta.CONFIRMADA.equals(novoStatus) && 
                    !StatusConsulta.CANCELADA.equals(novoStatus)) {
                    throw new IllegalStateException(
                        "Consulta AGENDADA só pode ir para CONFIRMADA ou CANCELADA. Status solicitado: " + novoStatus);
                }
                break;
                
            case CONFIRMADA:
                if (!StatusConsulta.AGENDADA.equals(novoStatus) && 
                    !StatusConsulta.CANCELADA.equals(novoStatus)) {
                    throw new IllegalStateException(
                        "Consulta CONFIRMADA só pode voltar para AGENDADA ou ir para CANCELADA. Status solicitado: " + novoStatus);
                }
                break;
                
            case REALIZADA:
                if (!StatusConsulta.PAGO.equals(novoStatus)) {
                    throw new IllegalStateException(
                        "Consulta REALIZADA só pode ir para PAGO. Status solicitado: " + novoStatus);
                }
                break;
                
            case PAGO:
            case CANCELADA:
                throw new IllegalStateException(
                    "Consultas com status " + statusAtual + " não podem ter o status alterado");
                
            default:
                throw new IllegalStateException("Status atual não reconhecido: " + statusAtual);
        }
    }

    /**
     * Gera descrição para o histórico baseado no novo status
     */
    private String gerarDescricaoHistorico(StatusConsulta novoStatus, String motivo) {
        switch (novoStatus) {
            case CONFIRMADA:
                return "Consulta confirmada";
            case CANCELADA:
                return "Consulta cancelada: " + motivo;
            case AGENDADA:
                return "Consulta voltou para agendada";
            case REALIZADA:
                return "Consulta realizada";
            case PAGO:
                return "Consulta marcada como paga";
            default:
                return "Status alterado para " + novoStatus;
        }
    }

    @RequiresTenant
    @CacheEvict(value = {"dashboard-admin-org", "dashboard-profissional", "dashboard-super-admin", "consultas-intervalo-org",
                          "consultas-hoje-org", "consultas-semana-org", "consultas-mes-org", "consultas-ano-org",
                          "horarios-ocupados-org", "disponibilidade-consulta"}, allEntries = true)
    @Transactional
    public Consulta atualizarConsultaByOrg(Long id, AtualizarConsultaRequest request) {
        log.info("consulta.atualizar inicio consultaId={} profissionalId={} pacienteId={} dataHora={} status={}",
                id, request.profissionalId(), request.pacienteId(), request.dataHora(), request.status());
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));

        // PAGO e CANCELADA são estados finais: nenhuma edição é permitida (mesma regra de validarTransicaoStatus).
        if (StatusConsulta.PAGO.equals(consulta.getStatus()) || StatusConsulta.CANCELADA.equals(consulta.getStatus())) {
            throw new IllegalStateException(
                "Consultas com status " + consulta.getStatus() + " não podem ser editadas");
        }

        Long orgId = tenantHelper.getCurrentTenantId();

        // Lock pessimista no profissional: serializa agendamentos/reagendamentos concorrentes dele.
        profissionalRepository.buscarParaAgendamentoComLock(request.profissionalId(), orgId);

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        // Paciente de outra clínica responde como inexistente
        Paciente paciente = pacienteRepository.findByPaciCodigoAndOrganizacao_IdWithEndereco(request.pacienteId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        // Status pedido passa pela mesma regra de transição do PATCH /status; igual ao atual é no-op.
        StatusConsulta novoStatus = consulta.getStatus();
        if (request.status() != null && !request.status().isBlank()) {
            StatusConsulta solicitado = parseStatus(request.status());
            if (solicitado != consulta.getStatus()) {
                validarTransicaoStatus(consulta.getStatus(), solicitado);
                novoStatus = solicitado;
            }
        }

        if (StatusConsulta.AGENDADA.equals(novoStatus) || StatusConsulta.CONFIRMADA.equals(novoStatus)) {
            int duracaoMinutos = request.duracaoMinutos() != null
                ? request.duracaoMinutos()
                : (consulta.getDuracaoMinutos() != null ? consulta.getDuracaoMinutos() : DURACAO_PADRAO_MINUTOS);
            validarSemConflitoDeHorario(orgId, request.profissionalId(), request.dataHora(), duracaoMinutos, id);
        }

        StatusConsulta statusAnterior = consulta.getStatus();

        consulta.setProfissional(profissional);
        consulta.setPaciente(paciente);
        consulta.setDataHora(request.dataHora());

        // Vinculos opcionais: quando o id nao vem no request, o valor atual e preservado
        // para que uma edicao parcial nao apague dados ja gravados.
        if (request.especialidadeId() != null) {
            Especialidade especialidade = especialidadeRepository.findById(request.especialidadeId())
                .orElseThrow(() -> new IllegalArgumentException("Especialidade nao encontrada"));
            consulta.setEspecialidade(especialidade);
        }

        if (request.formaPagamentoId() != null) {
            FormaPagamento formaPagamento = buscarFormaPagamentoDaClinica(request.formaPagamentoId(), orgId);
            consulta.setFormaPagamento(formaPagamento);
        }

        if (request.duracaoMinutos() != null) {
            consulta.setDuracaoMinutos(request.duracaoMinutos());
        }
        consulta.setObservacoes(request.observacoes());
        consulta.setValor(request.valor());

        consulta.setStatus(novoStatus);

        Consulta salva = consultaRepository.save(consulta);

        Usuario usuarioAtual = getUsuarioAtual();
        registrarHistorico(salva, statusAnterior, salva.getStatus(), "Consulta atualizada", usuarioAtual);

        log.info("consulta.atualizar ok consultaId={} status={}", salva.getId(), salva.getStatus());
        return salva;
    }
    
    // ==========================================
    // ESTATÍSTICAS POR PROFISSIONAL (usuarioId + orgId) - HOJE
    // ==========================================

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - Gestor
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para AdminOrg em uma única query.
     * Calcula a semana atual (segunda a domingo) e o dia atual, agrupando por status.
     *
     * @param organizacaoId ID da organização
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-admin-org", key = "@tenantHelper.getCurrentTenantIdOrNull()")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardAdminOrg(Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);
        LocalDate hoje = LocalDate.now();
        Periodo semana = Periodo.semanaDe(hoje);
        Periodo dia = Periodo.dia(hoje);
        LocalDate inicioSemana = semana.dataInicio();
        LocalDate fimSemana = semana.dataFim();

        LocalDateTime inicioSemanaLdt = semana.inicio();
        LocalDateTime fimSemanaLdt    = semana.fim();
        LocalDateTime inicioDiaLdt    = dia.inicio();
        LocalDateTime fimDiaLdt       = dia.fim();

        log.debug("consulta.dashboard buscar perfil=ADMIN_ORG semanaInicio={} semanaFim={} hoje={}",
                inicioSemana, fimSemana, hoje);

        List<Object[]> rows = consultaRepository.findEstatisticasDashboardByOrganizacao(
            organizacaoId, inicioSemanaLdt, fimSemanaLdt, inicioDiaLdt, fimDiaLdt
        );

        long consultasHoje = 0L;
        long consultasAguardando = 0L;
        long consultasAtendidas = 0L;
        long consultasSemana = 0L;
        long canceladosSemana = 0L;
        long confirmadosSemana = 0L;

        for (Object[] row : rows) {
            StatusConsulta status = (StatusConsulta) row[0];
            String periodo = (String) row[1];
            long quantidade = ((Number) row[2]).longValue();

            consultasSemana += quantidade;

            boolean isHoje = "HOJE".equals(periodo);

            if (isHoje) {
                consultasHoje += quantidade;
                if (status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA) {
                    consultasAguardando += quantidade;
                }
                if (status == StatusConsulta.REALIZADA) {
                    consultasAtendidas += quantidade;
                }
            }

            if (status == StatusConsulta.CANCELADA) {
                canceladosSemana += quantidade;
            }
            if (status == StatusConsulta.CONFIRMADA) {
                confirmadosSemana += quantidade;
            }
        }

        return EstatisticasDashboardAdminOrgResponse.builder()
            .consultasHoje(consultasHoje)
            .consultasAguardando(consultasAguardando)
            .consultasAtendidas(consultasAtendidas)
            .consultasSemana(consultasSemana)
            .canceladosSemana(canceladosSemana)
            .confirmadosSemana(confirmadosSemana)
            .build();
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - PROFISSIONAL
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para o Profissional em uma única query.
     * Filtra por usuario.id via JOIN com profissional — o profissional vê apenas seus próprios dados.
     *
     * @param usuarioId ID do usuário logado
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-profissional", key = "#usuarioId")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardProfissional(Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        Periodo semana = Periodo.semanaDe(hoje);
        Periodo dia = Periodo.dia(hoje);
        LocalDate inicioSemana = semana.dataInicio();
        LocalDate fimSemana = semana.dataFim();

        LocalDateTime inicioSemanaLdt = semana.inicio();
        LocalDateTime fimSemanaLdt    = semana.fim();
        LocalDateTime inicioDiaLdt    = dia.inicio();
        LocalDateTime fimDiaLdt       = dia.fim();

        log.debug("consulta.dashboard buscar perfil=PROFISSIONAL usuarioId={} semanaInicio={} semanaFim={} hoje={}",
                usuarioId, inicioSemana, fimSemana, hoje);

        List<Object[]> rows = consultaRepository.findEstatisticasDashboardByProfissional(
            usuarioId, inicioSemanaLdt, fimSemanaLdt, inicioDiaLdt, fimDiaLdt
        );

        long consultasHoje       = 0L;
        long consultasAguardando = 0L;
        long consultasAtendidas  = 0L;
        long consultasSemana     = 0L;
        long canceladosSemana    = 0L;
        long confirmadosSemana   = 0L;

        for (Object[] row : rows) {
            StatusConsulta status = (StatusConsulta) row[0];
            String periodo        = (String) row[1];
            long quantidade       = ((Number) row[2]).longValue();

            consultasSemana += quantidade;

            boolean isHoje = "HOJE".equals(periodo);

            if (isHoje) {
                consultasHoje += quantidade;
                if (status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA) {
                    consultasAguardando += quantidade;
                }
                if (status == StatusConsulta.REALIZADA) {
                    consultasAtendidas += quantidade;
                }
            }

            if (status == StatusConsulta.CANCELADA)  { canceladosSemana  += quantidade; }
            if (status == StatusConsulta.CONFIRMADA) { confirmadosSemana += quantidade; }
        }

        return EstatisticasDashboardAdminOrgResponse.builder()
            .consultasHoje(consultasHoje)
            .consultasAguardando(consultasAguardando)
            .consultasAtendidas(consultasAtendidas)
            .consultasSemana(consultasSemana)
            .canceladosSemana(canceladosSemana)
            .confirmadosSemana(confirmadosSemana)
            .build();
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - SUPER_ADMIN (global)
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para SuperAdmin em uma única query global.
     * Sem filtro de organização — abrange todas as consultas do sistema.
     *
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-super-admin", key = "'global'")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardSuperAdmin() {
        LocalDate hoje = LocalDate.now();
        Periodo semana = Periodo.semanaDe(hoje);
        Periodo dia = Periodo.dia(hoje);
        LocalDate inicioSemana = semana.dataInicio();
        LocalDate fimSemana = semana.dataFim();

        LocalDateTime inicioSemanaLdt = semana.inicio();
        LocalDateTime fimSemanaLdt    = semana.fim();
        LocalDateTime inicioDiaLdt    = dia.inicio();
        LocalDateTime fimDiaLdt       = dia.fim();

        log.debug("consulta.dashboard buscar perfil=SUPER_ADMIN semanaInicio={} semanaFim={} hoje={}",
                inicioSemana, fimSemana, hoje);

        List<Object[]> rows = consultaRepository.findEstatisticasDashboardGlobal(
            inicioSemanaLdt, fimSemanaLdt, inicioDiaLdt, fimDiaLdt
        );

        long consultasHoje      = 0L;
        long consultasAguardando = 0L;
        long consultasAtendidas  = 0L;
        long consultasSemana    = 0L;
        long canceladosSemana   = 0L;
        long confirmadosSemana  = 0L;

        for (Object[] row : rows) {
            StatusConsulta status  = (StatusConsulta) row[0];
            String periodo         = (String) row[1];
            long quantidade        = ((Number) row[2]).longValue();

            consultasSemana += quantidade;

            boolean isHoje = "HOJE".equals(periodo);

            if (isHoje) {
                consultasHoje += quantidade;
                if (status == StatusConsulta.AGENDADA || status == StatusConsulta.CONFIRMADA) {
                    consultasAguardando += quantidade;
                }
                if (status == StatusConsulta.REALIZADA) {
                    consultasAtendidas += quantidade;
                }
            }

            if (status == StatusConsulta.CANCELADA)  { canceladosSemana  += quantidade; }
            if (status == StatusConsulta.CONFIRMADA) { confirmadosSemana += quantidade; }
        }

        return EstatisticasDashboardAdminOrgResponse.builder()
            .consultasHoje(consultasHoje)
            .consultasAguardando(consultasAguardando)
            .consultasAtendidas(consultasAtendidas)
            .consultasSemana(consultasSemana)
            .canceladosSemana(canceladosSemana)
            .confirmadosSemana(confirmadosSemana)
            .build();
    }

    @Cacheable(value = "consultas-intervalo-org", key = "@tenantHelper.getCurrentTenantIdOrNull() + '-' + #dataInicio + '-' + #dataFim")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorOrganizacaoEIntervalo(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        tenantHelper.validateTenant(organizacaoId);
        Periodo periodo = Periodo.entre(dataInicio, dataFim);
        return consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(
            organizacaoId,
            periodo.inicio(),
            periodo.fimExclusivo()
        );
    }

    //==========================================FIM=========================================

    /**
     * Busca estatísticas de consultas por médico e intervalo de datas
     * Retorna uma lista de arrays onde:
     * - Posição 0: Status da consulta (enum StatusConsulta)
     * - Posição 1: Quantidade de consultas com esse status
     * 
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário (será usado para buscar o profissional via JOIN)
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de Object[] com [status, quantidade]
     */
    @Transactional(readOnly = true)
    public List<Object[]> buscarEstatisticasPorMedicoEIntervalo(
            Long organizacaoId, 
            Long usuarioId, 
            LocalDate dataInicio, 
            LocalDate dataFim) {
        
        log.debug("consulta.estatisticas buscar usuarioId={} inicio={} fim={}",
                usuarioId, dataInicio, dataFim);
        
        Periodo periodo = Periodo.entre(dataInicio, dataFim);
        
        return consultaRepository.findEstatisticasPorMedicoEIntervalo(
            organizacaoId,
            usuarioId,
            periodo.inicio(),
            periodo.fim()
        );
    }

    /**
     * Busca durações de consultas realizadas por médico e intervalo
     * Retorna lista bruta de durações para cálculo no frontend
     * 
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário (será usado para buscar o profissional via JOIN)
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de durações em minutos
     */
    @Transactional(readOnly = true)
    public List<Integer> buscarDuracoesConsultas(
            Long organizacaoId, 
            Long usuarioId, 
            LocalDate dataInicio, 
            LocalDate dataFim) {
        
        log.debug("consulta.duracoes buscar usuarioId={} dataInicio={} dataFim={}",
                usuarioId, dataInicio, dataFim);

        Periodo periodo = Periodo.entre(dataInicio, dataFim);

        List<Integer> duracoes = consultaRepository.findDuracoesConsultasPorMedicoEIntervalo(
            organizacaoId,
            usuarioId,
            periodo.inicio(),
            periodo.fim()
        );
        
        log.debug("consulta.duracoes resultado quantidade={}", duracoes.size());
        
        return duracoes;
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarClinicasEmIntervaloDeDatas(Long profissionalId, LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        log.debug("consulta.pesquisar inicio profissionalId={} dataInicio={} dataFim={} status={}",
                profissionalId, dataInicio, dataFim, status);

        StatusConsulta statusEnum = converterStatus(status);

        Periodo periodo = Periodo.entre(dataInicio, dataFim);

        return consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
            orgId,
            profissionalId,
            statusEnum,
            periodo.inicio(),
            periodo.fimExclusivo()
        );
    }

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorIntervalo(LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();
        
        StatusConsulta statusEnum = converterStatus(status);

        Periodo periodo = Periodo.entre(dataInicio, dataFim);

        return consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
            orgId,
            statusEnum,
            periodo.inicio(),
            periodo.fimExclusivo()
        );
    }
    
    /**
     * Converte o status recebido na requisicao para o enum correspondente.
     *
     * @param status nome do status; nulo, vazio ou "ALL" significa "sem filtro de status"
     * @return o enum correspondente ou null quando nao houver filtro
     * @throws IllegalArgumentException se o valor informado nao for um status valido
     */
    private StatusConsulta converterStatus(String status) {
        if (status == null || status.isBlank() || status.trim().equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            return StatusConsulta.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status de consulta invalido: " + status);
        }
    }

    private void registrarHistorico(Consulta consulta, StatusConsulta statusAnterior, 
                                     StatusConsulta statusNovo, String observacao, Usuario alteradoPor) {
        ConsultaHistorico historico = ConsultaHistorico.builder()
            .consulta(consulta)
            .statusAnterior(statusAnterior)
            .statusNovo(statusNovo)
            .observacao(observacao)
            .alteradoPor(alteradoPor)
            .build();
        log.debug("consulta.historico registrado consultaId={}", consulta.getId());
        historicoRepository.save(historico);
    }
    
    private Usuario getUsuarioAtual() {
        Long userId = TenantContext.getCurrentUser();
        if (userId != null) {
            return usuarioRepository.findById(userId).orElse(null);
        }
        return null;
    }

    // ========== MÉTODO PARA BUSCAR HORÁRIOS OCUPADOS ==========

    @RequiresTenant
    @Cacheable(value = "horarios-ocupados-org",
               key = "@tenantHelper.getCurrentTenantId() + '-' + #profissionalId + '-' + #data")
    @Transactional(readOnly = true)
    public List<String> buscarHorariosOcupados(Long profissionalId, String data) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("consulta.horariosOcupados buscar profissionalId={} data={}", profissionalId, data);

        // Converte a string da data para LocalDateTime
        LocalDateTime dataConsulta = LocalDate.parse(data).atStartOfDay();

        return consultaRepository.findHorariosOcupados(orgId, profissionalId, dataConsulta);
    }

    /**
     * Horários ocupados (AGENDADA/CONFIRMADA) de um profissional num dia, com início e duração.
     *
     * @param profissionalId ID do profissional
     * @param data           data no formato yyyy-MM-dd
     * @return lista ordenada por início
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<HorarioOcupadoResponse> buscarHorariosOcupadosDetalhado(Long profissionalId, String data) {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDateTime inicioDoDia = LocalDate.parse(data).atStartOfDay();

        return consultaRepository
            .findAtivasDoProfissionalNoPeriodo(orgId, profissionalId, inicioDoDia, inicioDoDia.plusDays(1))
            .stream()
            .map(c -> new HorarioOcupadoResponse(
                c.getDataHora().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                c.getDuracaoMinutos() != null ? c.getDuracaoMinutos() : DURACAO_PADRAO_MINUTOS))
            .toList();
    }

    @RequiresTenant
    @Cacheable(value = "disponibilidade-consulta",
               key = "@tenantHelper.getCurrentTenantId() + '-' + #medicoId + '-' + #data + '-' + #horario")
    @Transactional(readOnly = true)
    public boolean verificarDisponibilidade(String data, String horario, Long medicoId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("consulta.disponibilidade inicio profissionalId={} data={} horario={}", medicoId, data, horario);

        LocalDateTime dataHora;
        try {
            dataHora = LocalDateTime.parse(data + "T" + horario); // formato ISO
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("consulta.disponibilidade negado motivo=DATA_HORARIO_INVALIDOS data={} horario={}", data, horario);
            return false;
        }

        // Mesma duração e mesma regra de sobreposição do cadastro: o front não pode ver "livre"
        // um horário que o cadastro vai recusar por conflito.
        int duracaoMinutos = profissionalRepository.buscarClinicoIdByOrg(medicoId, orgId)
            .map(Profissional::getTempoConsultaMinutos)
            .orElse(DURACAO_PADRAO_MINUTOS);
        boolean ocupado = existeConflitoDeHorario(orgId, medicoId, dataHora, duracaoMinutos, null);

        log.debug("consulta.disponibilidade resultado ocupado={}", ocupado);
        return ocupado;
    }

    // ==========================================
    // BUSCAR CONSULTAS POR MÉDICO E PERÍODO
    // ==========================================

    /**
     * Busca consultas de um médico por período (diário, semanal, mensal, anual)
     * Calcula automaticamente o intervalo de datas baseado no tipo de período
     * 
     * @param usuarioId ID do usuário do profissional
     * @param tipoPeriodo Tipo do período: "diario", "semanal", "mensal", "anual"
     * @return Lista de consultas do médico no período especificado
     */
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorMedicoEPeriodo(Long usuarioId, String tipoPeriodo) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("consulta.buscar periodo usuarioId={} tipoPeriodo={}",
                usuarioId, tipoPeriodo);

        LocalDate hoje = LocalDate.now();
        Periodo periodo = switch (tipoPeriodo.toLowerCase()) {
            case "diario" -> Periodo.dia(hoje);
            case "semanal" -> Periodo.semanaDe(hoje);   // segunda a domingo
            case "mensal" -> Periodo.mesDe(hoje);
            case "anual" -> Periodo.anoDe(hoje);
            default -> {
                log.warn("consulta.periodo negado motivo=TIPO_INVALIDO tipo={} acao=USA_DIARIO", tipoPeriodo);
                yield Periodo.dia(hoje);
            }
        };
        LocalDateTime inicio = periodo.inicio();
        LocalDateTime fim = periodo.fim();

        log.debug("consulta.periodo calculado inicio={} fim={}", inicio, fim);
        return consultaRepository.findConsultasPorMedicoEPeriodo(orgId, usuarioId, inicio, fim);
    }

    /**
     * Converte uma String de data para o tipo Date
     * @param dataString String no formato "yyyy-MM-dd" ou "dd/MM/yyyy"
     * @return Date ou null se a string for nula ou vazia
     */
    private Date parseStringToDate(String dataString) {
        if (dataString == null || dataString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Tenta diferentes formatos de data
            SimpleDateFormat[] formatos = {
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("dd/MM/yyyy"),
                new SimpleDateFormat("dd-MM-yyyy")
            };
            
            for (SimpleDateFormat formato : formatos) {
                try {
                    return formato.parse(dataString.trim());
                } catch (ParseException e) {
                    // Continua para o próximo formato
                }
            }
            
            log.warn("consulta.data negado motivo=FORMATO_NAO_RECONHECIDO data={}", dataString);
            return null;
        } catch (Exception e) {
            log.warn("consulta.data negado motivo=ERRO_CONVERSAO data={} causa={}", dataString, e.getClass().getSimpleName());
            return null;
        }
    }

    // ==========================================
    // DELETAR CONSULTA COM REGISTROS RELACIONADOS
    // ==========================================

    /**
     * Exclui uma consulta que ainda não foi concluída (sem prontuário).
     * Se a consulta possuir prontuário dentista ou médico (dados clínicos),
     * a exclusão é bloqueada para preservar os registros clínicos.
     *
     * Registros excluídos em cascata:
     * - tb_termo_autorizacao (questionário de saúde)
     * - tb_planejamento_terapeutico (planejamentos vinculados à consulta)
     * - consulta_historico (histórico de status)
     * - consulta
     *
     * @param consultaId ID da consulta a ser excluída
     * @throws IllegalArgumentException se a consulta não for encontrada
     * @throws IllegalStateException se a consulta possuir prontuário (concluída)
     */
    @RequiresTenant
    @CacheEvict(value = {"dashboard-admin-org", "dashboard-profissional", "dashboard-super-admin", "consultas-intervalo-org",
                          "consultas-hoje-org", "consultas-semana-org", "consultas-mes-org", "consultas-ano-org",
                          "horarios-ocupados-org", "disponibilidade-consulta"}, allEntries = true)
    @Transactional
    public void deletarConsulta(Long consultaId) {
        log.info("consulta.excluir inicio consultaId={}", consultaId);
        Long orgId = tenantHelper.getCurrentTenantId();
        Consulta consulta = consultaRepository.findByIdAndOrganizacao_Id(consultaId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada ou não pertence à organização"));

        // Bloqueia exclusão se houver prontuário dentista (consulta concluída)
        boolean temProntuarioDentista = !prontuarioDentistaRepository.findByConsultaId(consultaId).isEmpty();
        if (temProntuarioDentista) {
            log.warn("consulta.excluir negado motivo=TEM_PRONTUARIO_ODONTOLOGICO consultaId={}", consultaId);
            throw new IllegalStateException("Não é possível excluir uma consulta que possui prontuário odontológico. Os dados clínicos devem ser preservados.");
        }

        // Bloqueia exclusão se houver prontuário médico (consulta concluída)
        boolean temProntuarioMedico = prontuarioRepository.findByConsulta_Id(consultaId) != null;
        if (temProntuarioMedico) {
            log.warn("consulta.excluir negado motivo=TEM_PRONTUARIO_MEDICO consultaId={}", consultaId);
            throw new IllegalStateException("Não é possível excluir uma consulta que possui prontuário médico. Os dados clínicos devem ser preservados.");
        }

        log.debug("consulta.excluir cascata consultaId={}", consultaId);

        // 1. Termo de autorização / questionário de saúde
        termoAutorizacaoRepository.deleteByConsultaId(consultaId);
        log.debug("consulta.excluir termos consultaId={}", consultaId);

        // 2. Planejamentos terapêuticos (via FK direta com consulta)
        planejamentoTerapeuticoRepository.deleteByConsultaId(consultaId);
        log.debug("consulta.excluir planejamentos consultaId={}", consultaId);

        // 3. Histórico de alterações de status
        historicoRepository.deleteByConsultaId(consultaId);
        log.debug("consulta.excluir historico consultaId={}", consultaId);

        // 4. Consulta
        consultaRepository.delete(consulta);
        log.info("consulta.excluir ok consultaId={}", consultaId);
    }

    /**
     * Extrai o ID do profissional a partir do usuário logado.
     * Se o usuário não for um profissional, retorna null.
     *
     * @param usuario Usuário logado
     * @return ID do profissional ou null se não for profissional
     */
    private Long extrairProfissionalId(br.com.saudeConecta.domain.usuario.Usuario usuario) {
        if (usuario == null || !usuario.isClinico()) {
            return null;
        }
        return profissionalRepository.findByUsuarioIdWithRelations(usuario.getId())
                .map(Profissional::getId)
                .orElse(null);
    }

    /**
     * Forma de pagamento global ou da própria clínica. Id de outra clínica (ou inexistente) é
     * recusado como "não encontrada", sem revelar que existe em outro tenant.
     *
     * @param formaPagamentoId ID enviado pelo cliente
     * @param orgId            organização do token
     * @return a forma de pagamento permitida
     * @throws IllegalArgumentException se não existir ou for de outra clínica
     */
    private FormaPagamento buscarFormaPagamentoDaClinica(Long formaPagamentoId, Long orgId) {
        return formaPagamentoRepository.findDisponivelParaOrganizacao(formaPagamentoId, orgId)
                .orElseThrow(() -> {
                    log.warn("consulta.formaPagamento negado motivo=NAO_ENCONTRADA_OU_OUTRA_CLINICA formaPagamentoId={}",
                            formaPagamentoId);
                    return new IllegalArgumentException("Forma de pagamento nao encontrada");
                });
    }
}
