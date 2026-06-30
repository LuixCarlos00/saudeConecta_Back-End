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
    
    @RequiresTenant
    @Cacheable(value = "consultas-hoje-org", key = "@tenantHelper.getCurrentTenantId()")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findConsultasHoje(orgId);
    }
    
    @RequiresTenant
    @Cacheable(value = "consultas-semana-org", key = "@tenantHelper.getCurrentTenantId()")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual() {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1); // Segunda-feira
        LocalDate fimSemana = inicioSemana.plusDays(6); // Domingo
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59));
    }
    
    @RequiresTenant
    @Cacheable(value = "consultas-mes-org", key = "@tenantHelper.getCurrentTenantId()")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual() {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1); // Primeiro dia do mês
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth()); // Último dia do mês
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));
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

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
            .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        if (consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                request.profissionalId(), orgId, request.dataHora(), StatusConsulta.AGENDADA)) {
            throw new IllegalStateException("Já existe consulta agendada para este horário");
        }
        
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
        
        Consulta consulta = Consulta.builder()
            .organizacao(organizacao)
            .profissional(profissional)
            .paciente(paciente)
            .especialidade(especialidade)
            .dataHora(request.dataHora())
            .duracaoMinutos(request.duracaoMinutos() != null ? request.duracaoMinutos() : profissional.getTempoConsultaMinutos())
            .observacoes(request.observacoes())
            .formaPagamento(formaPagamento)
            .valor(request.valor())
            .status(StatusConsulta.AGENDADA)
            .criadoPor(criadoPor)
            .build();
        
        Consulta salva = consultaRepository.save(consulta);
        registrarHistorico(salva, null, StatusConsulta.AGENDADA, "Consulta cadastrada", criadoPor);
        
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

        if (!StatusConsulta.AGENDADA.equals(consulta.getStatus())) {
            throw new IllegalStateException("Apenas consultas AGENDADAS podem ser concluídas");
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

        Long orgId = tenantHelper.getCurrentTenantId();

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
            .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        // Verificar se existe OUTRA consulta no mesmo horário (excluindo a consulta atual)
        boolean horarioOcupado = consultaRepository.findByProfissional_IdAndOrganizacao_IdAndDataHora(
            request.profissionalId(), orgId, request.dataHora()
        ).stream()
            .anyMatch(c -> !c.getId().equals(id) && c.getStatus() == StatusConsulta.AGENDADA);
        
        if (horarioOcupado) {
            throw new IllegalStateException("Já existe consulta agendada para este horário");
        }

        Especialidade especialidade = null;
        if (request.especialidadeId() != null) {
            especialidade = especialidadeRepository.findById(request.especialidadeId()).orElse(null);
        }

        FormaPagamento formaPagamento = null;
        if (request.formaPagamentoId() != null) {
            formaPagamento = formaPagamentoRepository.findById(request.formaPagamentoId()).orElse(null);
        }

        StatusConsulta statusAnterior = consulta.getStatus();

        consulta.setProfissional(profissional);
        consulta.setPaciente(paciente);
        consulta.setEspecialidade(especialidade);
        consulta.setDataHora(request.dataHora());
        if (request.duracaoMinutos() != null) {
            consulta.setDuracaoMinutos(request.duracaoMinutos());
        }
        consulta.setObservacoes(request.observacoes());
        consulta.setFormaPagamento(formaPagamento);
        consulta.setValor(request.valor());

        if (request.status() != null && !request.status().isBlank()) {
            consulta.setStatus(StatusConsulta.valueOf(request.status().toUpperCase()));
        }

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
    // BUSCAS DE ESTATISTICAS - Dashboard - Admin_ORGANIZACAO
    // ===============================================================

    /**
     * Retorna todas as estatísticas do dashboard para AdminOrg em uma única query.
     * Calcula a semana atual (segunda a domingo) e o dia atual, agrupando por status.
     *
     * @param organizacaoId ID da organização
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @Cacheable(value = "dashboard-admin-org", key = "#organizacaoId")
    @Transactional(readOnly = true)
    public EstatisticasDashboardAdminOrgResponse getEstatisticasDashboardAdminOrg(Long organizacaoId) {
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

    @Cacheable(value = "consultas-intervalo-org", key = "#organizacaoId + '-' + #dataInicio + '-' + #dataFim")
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorOrganizacaoEIntervalo(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
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

        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndProfissionalIdAndDataHoraBetweenWithRelations(
                orgId,
                    profissionalId,
                dataInicio.atStartOfDay(),
                dataFim.plusDays(1).atStartOfDay()
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank() && !status.trim().equalsIgnoreCase("ALL")) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByOrganizacaoIdAndProfissionalIdDirectAndStatusOptionalAndDataHoraBetween(
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
        
        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(
                orgId,
                dataInicio.atStartOfDay(),
                dataFim.plusDays(1).atStartOfDay()
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByStatusOptionalAndDataHoraBetweenWithRelations(
            orgId,
            statusEnum,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
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

    @RequiresTenant
    @Cacheable(value = "disponibilidade-consulta",
               key = "@tenantHelper.getCurrentTenantId() + '-' + #medicoId + '-' + #data + '-' + #horario")
    @Transactional(readOnly = true)
    public boolean verificarDisponibilidade(String data, String horario, Long medicoId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Verificando disponibilidade - profissional: {}, data: {}, horario: {}, organizacao: {}", medicoId, data, horario, orgId);

        try {
            // Combina data e horario para criar LocalDateTime
            String dataHoraStr = data + "T" + horario; // Usa T em vez de espaço para formato ISO
            LocalDateTime dataHora = LocalDateTime.parse(dataHoraStr);
            
            // Verifica se existe consulta AGENDADA para este profissional nesta organização, data e horário
            boolean existeConsultaAgendada = consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                medicoId, orgId, dataHora, StatusConsulta.AGENDADA);
            
            log.debug("Resultado verificacao - existe consulta agendada: {}", existeConsultaAgendada);
            return existeConsultaAgendada;
            
        } catch (Exception e) {
            log.error("Erro ao verificar disponibilidade para profissional {} na data {} horário {}", medicoId, data, horario, e);
            return false;
        }
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
}
