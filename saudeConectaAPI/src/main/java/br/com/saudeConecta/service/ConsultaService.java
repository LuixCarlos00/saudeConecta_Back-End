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
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.AtualizarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.CancelarConsultaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final TenantHelper tenantHelper;
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarTodas() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_IdWithRelations(orgId);
    }
    
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
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasHoje() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findConsultasHoje(orgId);
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDaSemanaAtual() {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1); // Segunda-feira
        LocalDate fimSemana = inicioSemana.plusDays(6); // Domingo
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicioSemana.atStartOfDay(), fimSemana.atTime(23, 59, 59));
    }
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasDoMesAtual() {
        Long orgId = tenantHelper.getCurrentTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1); // Primeiro dia do mês
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth()); // Último dia do mês
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicioMes.atStartOfDay(), fimMes.atTime(23, 59, 59));
    }
    
    @RequiresTenant
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

    @RequiresTenant
    @Transactional
    public Consulta cadastrarConsultaByOrg(AgendarConsultaRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando consulta na organização: {}", orgId);

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
    @Transactional
    public Consulta realizar(Long id, String observacoes) {
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));
        
        StatusConsulta statusAnterior = consulta.getStatus();
        consulta.setStatus(StatusConsulta.REALIZADA);
        if (observacoes != null) {
            consulta.setObservacoes(observacoes);
        }
        consultaRepository.save(consulta);
        
        registrarHistorico(consulta, statusAnterior, StatusConsulta.REALIZADA, "Consulta realizada", getUsuarioAtual());
        
        log.info("Consulta ID: {} realizada", id);
        return consulta;
    }

    @RequiresTenant
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
    

    


    @RequiresTenant
    @Transactional
    public Consulta atualizarConsultaByOrg(Long id, AtualizarConsultaRequest request) {
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));

        Long orgId = tenantHelper.getCurrentTenantId();

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
            .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));

        if (consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
            request.profissionalId(), orgId,request.dataHora(), StatusConsulta.AGENDADA)) {
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
    
    public List<ConsultaHistorico> buscarHistorico(Long consultaId) {
        return historicoRepository.findByConsulta_IdOrderByCreatedAtDesc(consultaId);
    }

    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    public Long contarConsultasHojePorOrganizacao(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByOrganizacao_IdAndDataHoraBetween(
            organizacaoId, 
            hoje.atStartOfDay(), 
            hoje.plusDays(1).atStartOfDay()
        );
    }



    public Long contarConsultasRealizadasHojePorOrganizacao(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween(
            organizacaoId,
            StatusConsulta.REALIZADA,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    public Long contarConsultasAgendadasHojePorOrganizacao(Long organizacaoId) {
        LocalDate hoje = LocalDate.now();
        return consultaRepository.countByOrganizacao_IdAndStatusAndDataHoraBetween(
            organizacaoId,
            StatusConsulta.AGENDADA,
            hoje.atStartOfDay(),
            hoje.plusDays(1).atStartOfDay()
        );
    }

    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorOrganizacaoEIntervalo(Long organizacaoId, LocalDate dataInicio, LocalDate dataFim) {
        return consultaRepository.findByOrganizacaoIdAndDataHoraBetweenWithRelations(
            organizacaoId,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
    }

    @Transactional(readOnly = true)
    public List<Consulta> buscarConsultasPorProfissionalEIntervalo(Long profissionalId, LocalDate dataInicio, LocalDate dataFim) {
        return consultaRepository.findByProfissionalIdAndDataHoraBetweenWithRelations(
            profissionalId,
            dataInicio.atStartOfDay(),
            dataFim.plusDays(1).atStartOfDay()
        );
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Consulta> pesquisarClinicasEmIntervaloDeDatas(Long profissionalId, LocalDate dataInicio, LocalDate dataFim, String status) {
        Long orgId = tenantHelper.getCurrentTenantId();

        if (status != null && status.trim().equalsIgnoreCase("ALL")) {
            return consultaRepository.findByOrganizacaoIdAndProfissionalIdAndDataHoraBetweenWithRelations(
                orgId,
                profissionalId,
                dataInicio.atStartOfDay(),
                dataFim.plusDays(1).atStartOfDay()
            );
        }

        StatusConsulta statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = StatusConsulta.valueOf(status.trim().toUpperCase());
        }

        return consultaRepository.findByOrganizacaoIdAndProfissionalIdAndStatusOptionalAndDataHoraBetweenWithRelations(
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
        log.info("Histórico registrado para consulta ID: {}", consulta.getId());
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
    @Transactional(readOnly = true)
    public List<String> buscarHorariosOcupados(Long profissionalId, String data) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando horários ocupados para profissional {} na data {} na organização: {}", profissionalId, data, orgId);

        // Converte a string da data para LocalDateTime
        LocalDateTime dataConsulta = LocalDate.parse(data).atStartOfDay();

        return consultaRepository.findHorariosOcupados(orgId, profissionalId, dataConsulta);
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public boolean verificarDisponibilidade(String data, String horario, Long medicoId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Verificando disponibilidade - profissional: {}, data: {}, horário: {}, organização: {}", medicoId, data, horario, orgId);

        try {
            // Combina data e horario para criar LocalDateTime
            String dataHoraStr = data + "T" + horario; // Usa T em vez de espaço para formato ISO
            LocalDateTime dataHora = LocalDateTime.parse(dataHoraStr);
            
            // Verifica se existe consulta AGENDADA para este profissional nesta organização, data e horário
            boolean existeConsultaAgendada = consultaRepository.existsByProfissional_IdAndOrganizacao_IdAndDataHoraAndStatus(
                medicoId, orgId, dataHora, StatusConsulta.AGENDADA);
            
            log.debug("Resultado verificação - existe consulta agendada: {}", existeConsultaAgendada);
            return existeConsultaAgendada;
            
        } catch (Exception e) {
            log.error("Erro ao verificar disponibilidade para profissional {} na data {} horário {}", medicoId, data, horario, e);
            return false;
        }
    }
}
