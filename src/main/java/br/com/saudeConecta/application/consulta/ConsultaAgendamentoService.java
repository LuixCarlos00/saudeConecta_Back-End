package br.com.saudeConecta.application.consulta;

import br.com.saudeConecta.application.consulta.dto.AgendarConsultaRequest;
import br.com.saudeConecta.application.consulta.dto.AtualizarConsultaRequest;
import br.com.saudeConecta.application.consulta.dto.HorarioOcupadoResponse;
import br.com.saudeConecta.config.tenant.RequiresTenant;
import br.com.saudeConecta.config.tenant.TenantContext;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.ConsultaHistorico;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.pagamento.FormaPagamento;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso que mudam a agenda: cadastrar, atualizar, mudar status e excluir consulta, além da
 * disponibilidade e dos horários ocupados (que usam a mesma regra de sobreposição do cadastro).
 *
 * <p>Saiu do antigo {@code ConsultaService} na fase B6 sem mudança de comportamento: o lock
 * pessimista no profissional ({@code buscarParaAgendamentoComLock}), as transações, os
 * {@code @CacheEvict} e as checagens de tenant são os mesmos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultaAgendamentoService {

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
    @CacheEvict(value = {"dashboard-admin-org", "dashboard-profissional", "dashboard-super-admin", "consultas-intervalo-org",
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

        int duracaoMinutos = resolverDuracaoMinutos(request.duracaoMinutos(), profissional.getTempoConsultaMinutos());

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
        Consulta consulta = buscarDaOrganizacao(id)
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
                
            case PAGO, CANCELADA:
                throw new IllegalStateException(
                    "Consultas com status " + statusAtual + " não podem ter o status alterado");
                
            default:
                throw new IllegalStateException("Status atual não reconhecido: " + statusAtual);
        }
    }

    /**
     * Duração efetiva: a informada na requisição, senão a já cadastrada, senão o padrão.
     *
     * @param solicitada duração enviada na requisição (pode ser nula)
     * @param cadastrada duração do profissional ou da consulta (pode ser nula)
     * @return duração em minutos
     */
    private int resolverDuracaoMinutos(Integer solicitada, Integer cadastrada) {
        if (solicitada != null) {
            return solicitada;
        }
        return cadastrada != null ? cadastrada : DURACAO_PADRAO_MINUTOS;
    }

    /**
     * Status resultante da edição: o pedido passa pela regra de transição; ausente ou igual ao atual é no-op.
     *
     * @param consulta consulta sendo editada
     * @param statusSolicitado status enviado na requisição (pode ser nulo ou em branco)
     * @return status que a consulta terá após a edição
     */
    private StatusConsulta resolverNovoStatus(Consulta consulta, String statusSolicitado) {
        if (statusSolicitado == null || statusSolicitado.isBlank()) {
            return consulta.getStatus();
        }
        StatusConsulta solicitado = parseStatus(statusSolicitado);
        if (solicitado == consulta.getStatus()) {
            return consulta.getStatus();
        }
        validarTransicaoStatus(consulta.getStatus(), solicitado);
        return solicitado;
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
        Consulta consulta = buscarDaOrganizacao(id)
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
        StatusConsulta novoStatus = resolverNovoStatus(consulta, request.status());

        if (StatusConsulta.AGENDADA.equals(novoStatus) || StatusConsulta.CONFIRMADA.equals(novoStatus)) {
            int duracaoMinutos = resolverDuracaoMinutos(request.duracaoMinutos(), consulta.getDuracaoMinutos());
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

    /**
     * Consulta da organização do token, sem fetch das relações. Era a chamada interna a
     * {@code buscarPorId} do antigo {@code ConsultaService}: roda dentro da transação de escrita
     * de quem chama, sem passar por proxy (mesmas queries e mesma transação de antes da quebra).
     *
     * @param id ID da consulta
     * @return a consulta, ou vazio se não existir ou for de outra clínica
     */
    private Optional<Consulta> buscarDaOrganizacao(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return consultaRepository.findByIdAndOrganizacao_Id(id, orgId);
    }
}
