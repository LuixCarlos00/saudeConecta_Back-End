package br.com.saudeConecta.service;

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
import br.com.saudeConecta.presentation.dto.consulta.CancelarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaDentistaResponse;
import br.com.saudeConecta.domain.prontuario.Prontuario;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<Consulta> buscarTodas(Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_Id(orgId, pageable);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Consulta> buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_Id(id, orgId);
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
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, referencia.atStartOfDay(), referencia.atTime(23, 59, 59));
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
        LocalDate inicioSemana = referencia.minusDays(referencia.getDayOfWeek().getValue() - 1); // Segunda-feira
        LocalDate fimSemana = inicioSemana.plusDays(6); // Domingo
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59));
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
        LocalDate inicioMes = referencia.withDayOfMonth(1); // Primeiro dia do mês
        LocalDate fimMes = referencia.withDayOfMonth(referencia.lengthOfMonth()); // Último dia do mês
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, profissionalId, inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));
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
        LocalDate hoje = LocalDate.now();
        LocalDate inicioAno = hoje.withDayOfYear(1); // Primeiro dia do ano
        LocalDate fimAno = hoje.withDayOfYear(hoje.lengthOfYear()); // Último dia do ano
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicioAno.atStartOfDay(), fimAno.atTime(23, 59, 59));
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarPorProfissional(Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_IdAndProfissional_IdWithRelations(orgId, profissionalId);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarAgendaDia(Long profissionalId, LocalDate data) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findAgendaDia(orgId, profissionalId, data);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicio, fim);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarPorPaciente(Long pacienteId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_IdAndPaciente_PaciCodigoWithRelations(orgId, pacienteId);
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
        
        log.info("Buscando consultas com filtros dinamicos - orgId: {}, profissionalId: {}, especialidade: {}, periodo: {} a {}, status: {}",
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
        log.info("Cadastrando consulta na organizacao: {}", orgId);

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        // Lock pessimista no profissional: serializa agendamentos concorrentes dele até o fim da transação.
        profissionalRepository.buscarParaAgendamentoComLock(request.profissionalId(), orgId);

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
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
            formaPagamento = formaPagamentoRepository.findById(request.formaPagamentoId()).orElse(null);
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
        
        log.info("Consulta cadastrada com sucesso. ID: {}", salva.getId());
        return salva;
    }

    @RequiresTenant
    @CacheEvict(value = {"dashboard-admin-org", "dashboard-profissional", "dashboard-super-admin", "consultas-intervalo-org",
                          "consultas-hoje-org", "consultas-semana-org", "consultas-mes-org", "consultas-ano-org",
                          "horarios-ocupados-org", "disponibilidade-consulta"}, allEntries = true)
    @Transactional
    public Consulta concluirConsultabyOrg(Long id) {

        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));

        if (!StatusConsulta.AGENDADA.equals(consulta.getStatus())
                && !StatusConsulta.CONFIRMADA.equals(consulta.getStatus())) {
            throw new IllegalStateException("Apenas consultas AGENDADAS ou CONFIRMADAS podem ser concluídas");
        }

        StatusConsulta statusAnterior = consulta.getStatus();
        consulta.setStatus(StatusConsulta.REALIZADA);
        Consulta salva = consultaRepository.save(consulta);

        registrarHistorico(salva, statusAnterior, StatusConsulta.REALIZADA, "Consulta concluída", getUsuarioAtual());

        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_IdWithRelations(salva.getId(), orgId)
            .orElse(salva);
    }
    

    

    /**
     * Atualiza o status de uma consulta seguindo as regras de transição:
     * - AGENDADA → CONFIRMADA, CANCELADA
     * - CONFIRMADA → AGENDADA, CANCELADA
     * - REALIZADA → PAGO (AdminOrg pode marcar como pago após médico concluir)
     * 
     * IMPORTANTE: Status REALIZADA só pode ser definido pelo médico através do método concluirConsultabyOrg
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
     * IMPORTANTE: Status REALIZADA não pode ser definido manualmente, apenas via concluirConsultabyOrg
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

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
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
            FormaPagamento formaPagamento = formaPagamentoRepository.findById(request.formaPagamentoId())
                .orElseThrow(() -> new IllegalArgumentException("Forma de pagamento nao encontrada"));
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

        return salva;
    }
    
    @RequiresTenant
    public Long contarAgendadasHoje() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.countAgendadasHoje(orgId);
    }

    // ==========================================
    // ESTATÍSTICAS POR PROFISSIONAL (usuarioId + orgId) - HOJE
    // ==========================================

    /**
     * Conta todas as consultas de hoje de um profissional específico na organização
     *
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário logado (profissional)
     * @return Quantidade de consultas hoje
     */
    public Long contarConsultasHojePorUsuarioEOrg(Long organizacaoId, Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countConsultasHojePorUsuarioEOrg(
            organizacaoId,
            usuarioId,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    /**
     * Conta consultas REALIZADAS hoje de um profissional específico na organização
     *
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário logado (profissional)
     * @return Quantidade de consultas realizadas hoje
     */
    public Long contarConsultasRealizadasHojePorUsuarioEOrg(Long organizacaoId, Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countConsultasRealizadasHojePorUsuarioEOrg(
            organizacaoId,
            usuarioId,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    /**
     * Conta consultas AGENDADAS hoje de um profissional específico na organização
     *
     * @param organizacaoId ID da organização
     * @param usuarioId ID do usuário logado (profissional)
     * @return Quantidade de consultas agendadas hoje
     */
    public Long contarConsultasAgendadasHojePorUsuarioEOrg(Long organizacaoId, Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countConsultasAgendadasHojePorUsuarioEOrg(
            organizacaoId,
            usuarioId,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

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
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);

        LocalDateTime inicioSemanaLdt = inicioSemana.atStartOfDay();
        LocalDateTime fimSemanaLdt = fimSemana.atTime(23, 59, 59);
        LocalDateTime inicioDiaLdt = hoje.atStartOfDay();
        LocalDateTime fimDiaLdt = hoje.atTime(23, 59, 59);

        log.debug("Buscando estatisticas dashboard AdminOrg - OrgId: {}, Semana: {} a {}, Hoje: {}",
                  organizacaoId, inicioSemana, fimSemana, hoje);

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
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);

        LocalDateTime inicioSemanaLdt = inicioSemana.atStartOfDay();
        LocalDateTime fimSemanaLdt    = fimSemana.atTime(23, 59, 59);
        LocalDateTime inicioDiaLdt    = hoje.atStartOfDay();
        LocalDateTime fimDiaLdt       = hoje.atTime(23, 59, 59);

        log.debug("Buscando estatisticas dashboard Profissional - UsuarioId: {}, Semana: {} a {}, Hoje: {}",
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
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);

        LocalDateTime inicioSemanaLdt = inicioSemana.atStartOfDay();
        LocalDateTime fimSemanaLdt    = fimSemana.atTime(23, 59, 59);
        LocalDateTime inicioDiaLdt    = hoje.atStartOfDay();
        LocalDateTime fimDiaLdt       = hoje.atTime(23, 59, 59);

        log.debug("Buscando estatisticas dashboard SuperAdmin - Semana: {} a {}, Hoje: {}",
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

    public Long getEstatisticaConsultasHojeByAdmiOrg(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByOrganizacao_IdAndDataHoraBetween(
            organizacaoId, 
            hoje.atStartOfDay(), 
            hoje.plusDays(1).atStartOfDay()
        );
    }

    public Long getEstatisticaConsultasAtendidasByAdmiOrg(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween(
            organizacaoId,
            StatusConsulta.REALIZADA,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    public Long getEstatisticasConsultaAgendadasHojeByOrd(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween(
            organizacaoId,
            StatusConsulta.AGENDADA,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    @Cacheable(value = "consultas-intervalo-org", key = "@tenantHelper.getCurrentTenantIdOrNull() + '-' + #dataInicio + '-' + #dataFim")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorOrganizacaoEIntervalo(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        tenantHelper.validateTenant(organizacaoId);
        return consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(
            organizacaoId,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
    }

    @Transactional(readOnly = true)
    public Long getEstatisticasSemanaPorOrganizacao(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);

        log.debug("Contando consultas da semana por organizacao - OrgId: {}, Inicio: {}, Fim: {}",
                organizacaoId, inicioSemana, fimSemana);

        return consultaRepository.countConsultasSemanaByOrganizacao(
                organizacaoId,
                inicioSemana.atStartOfDay(),
                fimSemana.atTime(23, 59, 59)
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
        
        log.debug("Buscando estatisticas - OrgId: {}, UsuarioId: {}, Inicio: {}, Fim: {}", 
                  organizacaoId, usuarioId, dataInicio, dataFim);
        
        return consultaRepository.findEstatisticasPorMedicoEIntervalo(
            organizacaoId,
            usuarioId,
            dataInicio.atStartOfDay(),
            dataFim.atTime(23, 59, 59)
        );
    }

    /**
     * Conta consultas da semana atual por médico e organização
     * Se profissionalId for null, conta todas as consultas da organização
     * 
     * @param organizacaoId ID da organização
     * @param profissionalId ID do profissional/médico (opcional)
     * @return Quantidade de consultas da semana
     */
    @Transactional(readOnly = true)
    public Long contarConsultasSemana(Long organizacaoId, Long profissionalId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);
        
        log.debug("Contando consultas da semana - OrgId: {}, ProfId: {}, Inicio: {}, Fim: {}", 
                  organizacaoId, profissionalId, inicioSemana, fimSemana);
        
        return consultaRepository.countConsultasPorMedicoEIntervalo(
            organizacaoId,
            profissionalId,
            inicioSemana.atStartOfDay(),
            fimSemana.atTime(23, 59, 59)
        );
    }

    /**
     * Conta consultas da semana atual de todas as organizações (SuperAdmin)
     *
     * @return Quantidade total de consultas da semana
     */
    @Transactional(readOnly = true)
    public Long contarConsultasSemanaGlobal() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);

        log.debug("Contando consultas da semana globalmente - Inicio: {}, Fim: {}",
                  inicioSemana, fimSemana);

        return consultaRepository.countConsultasSemanaGlobal(
            inicioSemana.atStartOfDay(),
            fimSemana.atTime(23, 59, 59)
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
        
        log.debug("Buscando duracoes de consultas - OrgId: {}, UsuarioId: {}, Inicio: {}, Fim: {}", 
                  organizacaoId, usuarioId, dataInicio, dataFim);

        List<Integer> duracoes = consultaRepository.findDuracoesConsultasPorMedicoEIntervalo(
            organizacaoId,
            usuarioId,
            dataInicio.atStartOfDay(),
            dataFim.atTime(23, 59, 59)
        );
        
        log.debug("Encontradas {} consultas com duracao", duracoes.size());
        
        return duracoes;
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarClinicasEmIntervaloDeDatas(Long profissionalId, LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        log.debug("Pesquisando consultas - OrgId: {}, UsuarioId: {}, Inicio: {}, Fim: {}, Status: {}", 
                  orgId, profissionalId, dataInicio, dataFim, status);

        StatusConsulta statusEnum = converterStatus(status);

        return consultaRepository.findByOrganizacaoIdAndUsuarioIdAndStatusOptionalAndDataHoraBetween(
            orgId,
            profissionalId,
            statusEnum,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarMedicoEspecialidadeEmIntervaloDeDatas(Long profissionalId, String especialidade, LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.
                    findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations(
                orgId,
                profissionalId,
                especialidade,
                dataInicio.atStartOfDay(),
                dataFim.plusDays(1).atStartOfDay()
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.
                findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
            orgId,
            profissionalId,
            especialidade,
            statusEnum,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarEspecialidadeEmIntervaloDeDatas(String especialidade, LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndDataHoraBetweenWithRelations(
                orgId,
                especialidade,
                dataInicio.atStartOfDay(),
                dataFim.plusDays(1).atStartOfDay()
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalAndDataHoraBetweenWithRelations(
            orgId,
            especialidade,
            statusEnum,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarPorProfissionalEEspecialidade(Long profissionalId, String especialidade, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingWithRelations(
                orgId,
                profissionalId,
                especialidade
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByOrganizacaoIdAndProfissionalIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
            orgId,
            profissionalId,
            especialidade,
            statusEnum
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarPorEspecialidade(String especialidade, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingWithRelations(
                orgId,
                especialidade
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
            orgId,
            especialidade,
            statusEnum
        );
    }

    @Transactional(readOnly = true)
    public List<Consulta> pesquisarPorEspecialidadeEOrganizacao(Long organizacaoId, String especialidade, String status) {
        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingWithRelations(
                organizacaoId,
                especialidade
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByOrganizacaoIdAndEspecialidadeNomeContainingAndStatusOptionalWithRelations(
            organizacaoId,
            especialidade,
            statusEnum
        );
    }

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    public Long contarTodasConsultasHoje() {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByDataHoraBetween(
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    public Long contarTodasConsultasRealizadasHoje() {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByStatusAndDataHoraBetween(
            StatusConsulta.REALIZADA,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    public Long contarTodasConsultasAgendadasHoje() {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByStatusAndDataHoraBetween(
            StatusConsulta.AGENDADA,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorIntervalo(LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();
        
        StatusConsulta statusEnum = converterStatus(status);

        return consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
            orgId,
            statusEnum,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
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
        log.info("Historico registrado para consulta ID: {}", consulta.getId());
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
        log.debug("Buscando horarios ocupados para profissional {} na data {} na organizacao: {}", profissionalId, data, orgId);

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
        log.debug("Verificando disponibilidade - profissional: {}, data: {}, horario: {}, organizacao: {}", medicoId, data, horario, orgId);

        LocalDateTime dataHora;
        try {
            dataHora = LocalDateTime.parse(data + "T" + horario); // formato ISO
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("Data/horario invalidos na verificacao de disponibilidade: data={} horario={}", data, horario);
            return false;
        }

        // Mesma duração e mesma regra de sobreposição do cadastro: o front não pode ver "livre"
        // um horário que o cadastro vai recusar por conflito.
        int duracaoMinutos = profissionalRepository.buscarClinicoIdByOrg(medicoId, orgId)
            .map(Profissional::getTempoConsultaMinutos)
            .orElse(DURACAO_PADRAO_MINUTOS);
        boolean ocupado = existeConflitoDeHorario(orgId, medicoId, dataHora, duracaoMinutos, null);

        log.debug("Resultado verificacao - horario ocupado: {}", ocupado);
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
        log.debug("Buscando consultas para usuario {} com periodo {} na organizacao {}", 
                  usuarioId, tipoPeriodo, orgId);

        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio;
        LocalDateTime fim;

        switch (tipoPeriodo.toLowerCase()) {
            case "diario":
                // Hoje: 00:00:00 até 23:59:59
                inicio = hoje.atStartOfDay();
                fim = hoje.atTime(23, 59, 59);
                break;

            case "semanal":
                // Semana atual: Segunda-feira até Domingo
                LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
                LocalDate fimSemana = inicioSemana.plusDays(6);
                inicio = inicioSemana.atStartOfDay();
                fim = fimSemana.atTime(23, 59, 59);
                break;

            case "mensal":
                // Mês atual: Primeiro dia até último dia
                LocalDate inicioMes = hoje.withDayOfMonth(1);
                LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
                inicio = inicioMes.atStartOfDay();
                fim = fimMes.atTime(23, 59, 59);
                break;

            case "anual":
                // Ano atual: 01/01 até 31/12
                LocalDate inicioAno = hoje.withDayOfYear(1);
                LocalDate fimAno = hoje.withDayOfYear(hoje.lengthOfYear());
                inicio = inicioAno.atStartOfDay();
                fim = fimAno.atTime(23, 59, 59);
                break;

            default:
                log.warn("Tipo de periodo invalido: {}. Usando 'diario' como padrao.", tipoPeriodo);
                inicio = hoje.atStartOfDay();
                fim = hoje.atTime(23, 59, 59);
        }

        log.debug("Periodo calculado: {} ate {}", inicio, fim);
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
            
            log.warn("Formato de data nao reconhecido: {}", dataString);
            return null;
        } catch (Exception e) {
            log.error("Erro ao converter data: {}", dataString, e);
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
        Long orgId = tenantHelper.getCurrentTenantId();
        Consulta consulta = consultaRepository.findByIdAndOrganizacao_Id(consultaId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada ou não pertence à organização"));

        // Bloqueia exclusão se houver prontuário dentista (consulta concluída)
        boolean temProntuarioDentista = !prontuarioDentistaRepository.findByConsultaId(consultaId).isEmpty();
        if (temProntuarioDentista) {
            throw new IllegalStateException("Não é possível excluir uma consulta que possui prontuário odontológico. Os dados clínicos devem ser preservados.");
        }

        // Bloqueia exclusão se houver prontuário médico (consulta concluída)
        boolean temProntuarioMedico = prontuarioRepository.findByConsulta_Id(consultaId) != null;
        if (temProntuarioMedico) {
            throw new IllegalStateException("Não é possível excluir uma consulta que possui prontuário médico. Os dados clínicos devem ser preservados.");
        }

        log.info("Iniciando exclusao da consulta {} e registros relacionados", consultaId);

        // 1. Termo de autorização / questionário de saúde
        termoAutorizacaoRepository.deleteByConsultaId(consultaId);
        log.debug("Termos de autorizacao excluidos para consulta {}", consultaId);

        // 2. Planejamentos terapêuticos (via FK direta com consulta)
        planejamentoTerapeuticoRepository.deleteByConsultaId(consultaId);
        log.debug("Planejamentos excluidos para consulta {}", consultaId);

        // 3. Histórico de alterações de status
        historicoRepository.deleteByConsultaId(consultaId);
        log.debug("Historico de consulta excluido para consulta {}", consultaId);

        // 4. Consulta
        consultaRepository.delete(consulta);
        log.info("Consulta {} excluida com sucesso", consultaId);
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
}
