package br.com.saudeConecta.application.service;

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
    public List<Consulta> buscarTodas() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_Id(orgId);
    }
    
    @RequiresTenant
    public Page<Consulta> buscarTodas(Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_Id(orgId, pageable);
    }
    
    @RequiresTenant
    public Optional<Consulta> buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_Id(id, orgId);
    }
    
    @RequiresTenant
    public List<Consulta> buscarConsultasHoje() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findConsultasHoje(orgId);
    }
    
    @RequiresTenant
    public List<Consulta> buscarPorProfissional(Long profissionalId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_IdAndProfissional_Id(orgId, profissionalId);
    }
    
    @RequiresTenant
    public List<Consulta> buscarAgendaDia(Long profissionalId, LocalDate data) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findAgendaDia(orgId, profissionalId, data);
    }
    
    @RequiresTenant
    public List<Consulta> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacaoIdAndPeriodo(orgId, inicio, fim);
    }
    
    @RequiresTenant
    public List<Consulta> buscarPorPaciente(Long pacienteId) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByOrganizacao_IdAndPaciente_PaciCodigo(orgId, pacienteId);
    }
    
    @RequiresTenant
    @Transactional
    public Consulta agendar(AgendarConsultaRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Agendando consulta na organização: {}", orgId);
        
        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));
        
        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(request.profissionalId(), orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
        
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
            .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        
        if (consultaRepository.existsByProfissional_IdAndDataHoraAndStatusNot(
                request.profissionalId(), request.dataHora(), StatusConsulta.CANCELADA)) {
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
        registrarHistorico(salva, null, StatusConsulta.AGENDADA, "Consulta agendada", criadoPor);
        
        log.info("Consulta agendada com sucesso. ID: {}", salva.getId());
        return salva;
    }
    
    @RequiresTenant
    @Transactional
    public Consulta confirmar(Long id) {
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));
        
        if (!consulta.isAgendada()) {
            throw new IllegalStateException("Apenas consultas agendadas podem ser confirmadas");
        }
        
        StatusConsulta statusAnterior = consulta.getStatus();
        consulta.setStatus(StatusConsulta.CONFIRMADA);
        consultaRepository.save(consulta);
        
        registrarHistorico(consulta, statusAnterior, StatusConsulta.CONFIRMADA, "Consulta confirmada", getUsuarioAtual());
        
        log.info("Consulta ID: {} confirmada", id);
        return consulta;
    }
    
    @RequiresTenant
    @Transactional
    public Consulta iniciar(Long id) {
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));
        
        if (!consulta.isConfirmada() && !consulta.isAgendada()) {
            throw new IllegalStateException("Consulta não pode ser iniciada");
        }
        
        StatusConsulta statusAnterior = consulta.getStatus();
        consulta.setStatus(StatusConsulta.EM_ANDAMENTO);
        consultaRepository.save(consulta);
        
        registrarHistorico(consulta, statusAnterior, StatusConsulta.EM_ANDAMENTO, "Consulta iniciada", getUsuarioAtual());
        
        log.info("Consulta ID: {} iniciada", id);
        return consulta;
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
    public Consulta cancelar(Long id, CancelarConsultaRequest request) {
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));
        
        if (!consulta.podeSerCancelada()) {
            throw new IllegalStateException("Esta consulta não pode ser cancelada");
        }
        
        StatusConsulta statusAnterior = consulta.getStatus();
        consulta.setStatus(StatusConsulta.CANCELADA);
        consulta.setCanceladoPor(CanceladoPor.valueOf(request.canceladoPor().toUpperCase()));
        consulta.setMotivoCancelamento(request.motivo());
        consultaRepository.save(consulta);
        
        registrarHistorico(consulta, statusAnterior, StatusConsulta.CANCELADA, 
            "Cancelado por " + request.canceladoPor() + ": " + request.motivo(), getUsuarioAtual());
        
        log.info("Consulta ID: {} cancelada", id);
        return consulta;
    }
    
    @RequiresTenant
    @Transactional
    public Consulta marcarNaoCompareceu(Long id) {
        Consulta consulta = buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada"));
        
        StatusConsulta statusAnterior = consulta.getStatus();
        consulta.setStatus(StatusConsulta.NAO_COMPARECEU);
        consultaRepository.save(consulta);
        
        registrarHistorico(consulta, statusAnterior, StatusConsulta.NAO_COMPARECEU, "Paciente não compareceu", getUsuarioAtual());
        
        log.info("Consulta ID: {} marcada como não compareceu", id);
        return consulta;
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

    @Transactional(readOnly = true)
    public List<Consulta> buscarTodasConsultasPorIntervalo(LocalDate dataInicio, LocalDate dataFim) {
        return consultaRepository.findByDataHoraBetweenWithRelations(
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
        historicoRepository.save(historico);
    }
    
    private Usuario getUsuarioAtual() {
        Long userId = TenantContext.getCurrentUser();
        if (userId != null) {
            return usuarioRepository.findById(userId).orElse(null);
        }
        return null;
    }
}
