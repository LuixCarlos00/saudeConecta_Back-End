package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.presentation.dto.consulta.*;
import br.com.saudeConecta.presentation.dto.dashboard.SaldoFinanceiroResponse;
import br.com.saudeConecta.service.ConsultaService;
import br.com.saudeConecta.usecase.BuscarHistoricoCompletoPacienteAdminUseCase;
import br.com.saudeConecta.usecase.BuscarHistoricoCompletoPacienteDentistaUseCase;
import br.com.saudeConecta.usecase.BuscarHistoricoCompletoPacienteMedicoUseCase;
import br.com.saudeConecta.service.SaldoFinanceiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import br.com.saudeConecta.util.MascaraLog;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
@Slf4j
public class ConsultaController {
    
    private final ConsultaService consultaService;
    private final SaldoFinanceiroService saldoFinanceiroService;
    private final BuscarHistoricoCompletoPacienteDentistaUseCase buscarHistoricoCompletoPacienteDentistaUseCase;
    private final BuscarHistoricoCompletoPacienteMedicoUseCase buscarHistoricoCompletoPacienteMedicoUseCase;
    private final BuscarHistoricoCompletoPacienteAdminUseCase buscarHistoricoCompletoPacienteAdminUseCase;
    private final ProfissionalRepository profissionalRepository;
    private final TenantHelper tenantHelper;

//=================Tela de /gerenciamento =================
    @GetMapping("/hoje")
    public ResponseEntity<List<ConsultaResponse>> listarConsultasHoje(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        List<ConsultaResponse> response = consultaService.buscarConsultasHoje(data, usuarioLogado).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/semana-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDaSemanaAtual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        List<ConsultaResponse> response =
                consultaService.buscarConsultasDaSemanaAtual(data, usuarioLogado).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/mes-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDoMesAtual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        List<ConsultaResponse> response =
                consultaService.buscarConsultasDoMesAtual(data, usuarioLogado).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ano-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDoAnoAtual() {
        List<ConsultaResponse> response =
                consultaService.buscarConsultasDoAnoAtual().stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    //=========================================Tela de /gerenciamento =========================================
//=================================================CRUD =========================================

    @PostMapping("/cadastrarConsultaByOrg")
    public ResponseEntity<ConsultaResponse> cadastrarConsultaByOrg(@Valid @RequestBody AgendarConsultaRequest request) {
        Consulta consulta = consultaService.cadastrarConsultaByOrg(request);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    @PutMapping("/atualizarConsultabyOrg/{id}")
    public ResponseEntity<ConsultaResponse> atualizarConsultabyOrg(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConsultaRequest request) {
        Consulta consulta = consultaService.atualizarConsultaByOrg(id, request);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    @PutMapping("/concluirConsultabyOrg/{id}")
    public ResponseEntity<ConsultaResponse> concluirConsultabyOrg(@PathVariable Long id) {
        Consulta consulta = consultaService.concluirConsultabyOrg(id);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    /**
     * Exclui uma consulta e registros associados (questionário, planejamentos, histórico).
     * Consultas com prontuário (dentista ou médico) não podem ser excluídas.
     *
     * @param id ID da consulta a ser excluída
     * @return 204 No Content em caso de sucesso, 404 se não encontrada, 409 se possuir prontuário
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarConsulta(@PathVariable Long id) {
        // Exclusão bloqueada (IllegalStateException) vira 409 com mensagem no GlobalExceptionHandler
        try {
            consultaService.deletarConsulta(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("consulta.excluir negado motivo=NAO_ENCONTRADA consultaId={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    //===============================================CRUD ==================================================
    //=======================Atualizar  Status ==========================

    /**
     * Altera o status de uma consulta seguindo as regras de transição:
     * - AGENDADA → CONFIRMADA, CANCELADA
     * - CONFIRMADA → AGENDADA, CANCELADA
     * - REALIZADA → PAGO (AdminOrg pode marcar como pago após médico concluir)
     * 
     * IMPORTANTE: Status REALIZADA só pode ser definido pelo médico através do endpoint PUT /concluirConsultabyOrg/{id}
     *
     * @param id     ID da consulta
     * @param status Novo status (AGENDADA, CONFIRMADA, CANCELADA, PAGO)
     * @param motivo Motivo (obrigatório apenas para CANCELADA)
     * @return Consulta atualizada
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ConsultaResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String motivo) {
        try {
            StatusConsulta novoStatus = StatusConsulta.valueOf(status.toUpperCase());
            Consulta consulta = consultaService.atualizarStatus(id, novoStatus, motivo);
            return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
        } catch (IllegalArgumentException e) {
            log.warn("consulta.status negado motivo=STATUS_INVALIDO_OU_NAO_ENCONTRADA consultaId={} status={} detalhe={}",
                    id, MascaraLog.seguro(status), MascaraLog.seguro(e.getMessage(), 120));
            return ResponseEntity.badRequest().build();
        }
    }
    //=======================Atualizar  Status ==========================
    //=======================Validações para cadastro de consultas ==========================

    /**
     * Busca os horários já ocupados de um médico em uma data específica.
     * Utilizado para montar um select com os horários disponíveis (aqueles que não estão no banco).
     *
     * @param medicoId ID do médico/profissional
     * @param data     Data da consulta no formato yyyy-MM-dd
     * @return Lista de horários ocupados no formato HH:mm
     */
    @GetMapping("/horarios-ocupados")
    public ResponseEntity<List<String>> buscarHorariosOcupados(
            @RequestParam Long medicoId,
            @RequestParam String data) {
        List<String> horariosOcupados = consultaService.buscarHorariosOcupados(medicoId, data);
        return ResponseEntity.ok(horariosOcupados);
    }

    /**
     * Versão detalhada de /horarios-ocupados: devolve início e duração de cada consulta ocupando a agenda,
     * para o front bloquear o intervalo inteiro (e não só o horário de início).
     *
     * @param medicoId ID do médico/profissional
     * @param data     Data no formato yyyy-MM-dd
     * @return Lista de {inicio (HH:mm:ss), duracaoMinutos}
     */
    @GetMapping("/horarios-ocupados-detalhado")
    public ResponseEntity<List<HorarioOcupadoResponse>> buscarHorariosOcupadosDetalhado(
            @RequestParam Long medicoId,
            @RequestParam String data) {
        return ResponseEntity.ok(consultaService.buscarHorariosOcupadosDetalhado(medicoId, data));
    }

    /**
     * Verifica a disponibilidade de um médico para uma consulta específica.
     * Valida os critérios antes de cadastrar uma nova consulta.
     *
     * @param data     Data da consulta no formato yyyy-MM-dd
     * @param horario  Horário da consulta no formato HH:mm
     * @param medicoId ID do médico/profissional
     * @return true se horário está disponível, false se já existe consulta
     */
    @GetMapping("/verificarDisponibilidade")
    public ResponseEntity<Boolean> verificarDisponibilidade(
            @RequestParam String data,
            @RequestParam String horario,
            @RequestParam Long medicoId) {
        boolean existeConsulta = consultaService.verificarDisponibilidade(data, horario, medicoId);
        return ResponseEntity.ok(existeConsulta);
    }

    /**    //======================= Filtros  de pesquisas Dinamico ==========================

     * Endpoint dinâmico para buscar consultas com filtros opcionais
     * Todos os parâmetros são opcionais, permitindo qualquer combinação de filtros
     * 
     * @param profissionalId ID do profissional/médico (opcional)
     * @param especialidade Nome da especialidade (opcional)
     * @param dataInicial Data inicial do período (opcional)
     * @param dataFinal Data final do período (opcional)
     * @param status Lista de status separados por vírgula (opcional) - ex: "AGENDADA,CONFIRMADA"
     * @return Lista de consultas que atendem aos filtros
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ConsultaResponse>> buscarComFiltrosDinamicos(
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) String especialidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        
        
        // Converter datas para LocalDateTime
        LocalDateTime inicio = dataInicial != null ? dataInicial.atStartOfDay() : null;
        LocalDateTime fim = dataFinal != null ? dataFinal.atTime(23, 59, 59) : null;
        
        // Converter string de status para lista de StatusConsulta
        List<StatusConsulta> statusList = null;
        if (status != null && !status.trim().isEmpty()) {
            statusList = java.util.Arrays.stream(status.split(","))
                    .map(String::trim)
                    .map(StatusConsulta::valueOf)
                    .toList();
        }
        
        List<ConsultaResponse> response = consultaService.buscarComFiltrosDinamicos(
                profissionalId,
                especialidade,
                inicio,
                fim,
                statusList
        ).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        
        return ResponseEntity.ok(response);
    }
    //======================= Filtros  de pesquisas Dinamico ==========================

    @GetMapping("/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarTodasPorIntervalo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        List<ConsultaResponse> response = consultaService.buscarConsultasPorIntervalo(dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<ConsultaResponse>> listarPorProfissional(@PathVariable Long profissionalId) {
        List<ConsultaResponse> response = consultaService.buscarPorProfissional(profissionalId).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
// TODO verificar quais metodo estao sendo usados no front e com back , altera a url dels com o nome do metodo no front e back para vinculação

    /**
     * Busca consultas por médico e intervalo de datas
     * 
     * @param profissionalId ID do usuário do médico (será usado para buscar o profissional via JOIN)
     * @param dataInicial Data inicial do período
     * @param dataFinal Data final do período
     * @param status Status da consulta (opcional, use "ALL" para todos)
     * @return Lista de consultas
     */
    @GetMapping("/profissional/{profissionalId}/intervalo")
    public ResponseEntity<List<ConsultaResponse>> pesquisarClinicasEmIntervaloDeDatas(
            @PathVariable Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        List<ConsultaResponse> response = consultaService.pesquisarClinicasEmIntervaloDeDatas(
                        profissionalId, dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profissional/{profissionalId}/especialidade/intervalo")
    public ResponseEntity<List<ConsultaResponse>> pesquisarMedicoEspecialidadeEmIntervaloDeDatas(
            @PathVariable Long profissionalId,
            @RequestParam String especialidade,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        List<ConsultaResponse> response = consultaService.pesquisarMedicoEspecialidadeEmIntervaloDeDatas(
                        profissionalId, especialidade, dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profissional/{profissionalId}/especialidade")
    public ResponseEntity<List<ConsultaResponse>> pesquisarPorProfissionalEEspecialidade(
            @PathVariable Long profissionalId,
            @RequestParam String especialidade,
            @RequestParam(required = false) String status) {
        List<ConsultaResponse> response = consultaService.pesquisarPorProfissionalEEspecialidade(
                        profissionalId, especialidade, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/especialidade/intervalo")
    public ResponseEntity<List<ConsultaResponse>> pesquisarEspecialidadeEmIntervaloDeDatas(
            @RequestParam String especialidade,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        List<ConsultaResponse> response = consultaService.pesquisarEspecialidadeEmIntervaloDeDatas(
                        especialidade, dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/especialidade/{especialidade}")
    public ResponseEntity<List<ConsultaResponse>> pesquisarPorEspecialidade(
            @PathVariable String especialidade,
            @RequestParam(required = false) String status) {
        List<ConsultaResponse> response = consultaService.pesquisarPorEspecialidade(especialidade, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizacao/{organizacaoId}/especialidade/{especialidade}")
    public ResponseEntity<List<ConsultaResponse>> pesquisarPorEspecialidadeEOrganizacao(
            @PathVariable Long organizacaoId,
            @PathVariable String especialidade,
            @RequestParam(required = false) String status) {
        tenantHelper.validateTenant(organizacaoId);
        List<ConsultaResponse> response = consultaService.pesquisarPorEspecialidadeEOrganizacao(
                        organizacaoId, especialidade, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

//============================================================

    @GetMapping("/paginado")
    public ResponseEntity<Page<ConsultaResponse>> listarTodasPaginado(Pageable pageable) {
        Page<ConsultaResponse> response = consultaService.buscarTodas(pageable)
            .map(ConsultaResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        return consultaService.buscarPorId(id)
            .map(ConsultaResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    

    
    @GetMapping("/profissional/{profissionalId}/agenda")
    public ResponseEntity<List<ConsultaResponse>> buscarAgendaDia(
            @PathVariable Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<ConsultaResponse> response = consultaService.buscarAgendaDia(profissionalId, data).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/periodo")
    public ResponseEntity<List<ConsultaResponse>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        List<ConsultaResponse> response = consultaService.buscarPorPeriodo(inicio, fim).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<ConsultaResponse>> listarPorPaciente(@PathVariable Long pacienteId) {
        List<ConsultaResponse> response = consultaService.buscarPorPaciente(pacienteId).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    

    @GetMapping("/count/hoje")
    public ResponseEntity<Long> contarAgendadasHoje() {
        return ResponseEntity.ok(consultaService.contarAgendadasHoje());
    }

    /**
     * Busca consultas de um médico por período (diário, semanal, mensal, anual)
     * O backend calcula automaticamente o intervalo de datas baseado no tipo de período
     * 
     * @param usuarioId ID do usuário do profissional
     * @param tipoPeriodo Tipo do período: "diario", "semanal", "mensal", "anual" (padrão: "diario")
     * @return Lista de consultas do médico no período especificado
     */
    @GetMapping("/agenda-medico/{usuarioId}")
    public ResponseEntity<List<ConsultaResponse>> buscarAgendaMedicoPorPeriodo(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "diario") String tipoPeriodo) {
        List<ConsultaResponse> response = consultaService.buscarConsultasPorMedicoEPeriodo(usuarioId, tipoPeriodo)
                .stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - Gestor
    // ===============================================================

    /**
     * Endpoint único que retorna todas as estatísticas do dashboard para AdminOrg.
     * Realiza uma única query ao banco agrupando por status e período (hoje vs semana).
     *
     * @param organizacaoId ID da organização
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @GetMapping("/estatisticas/organizacao/{organizacaoId}/dashboard")
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardAdminOrg(
            @PathVariable Long organizacaoId) {
        // Validado antes do service: o @Cacheable usa o orgId como chave e responderia do cache sem checar.
        tenantHelper.validateTenant(organizacaoId);


        EstatisticasDashboardAdminOrgResponse response =
            consultaService.getEstatisticasDashboardAdminOrg(organizacaoId);

        return ResponseEntity.ok(response);
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - PROFISSIONAL
    // ===============================================================

    /**
     * Endpoint único que retorna todas as estatísticas do dashboard para o Profissional.
     * Filtra por usuario.id via JOIN — o profissional vê apenas seus próprios dados.
     *
     * @param usuarioId ID do usuário logado
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @GetMapping("/estatisticas/dashboard/profissional")
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardProfissional(
            @RequestParam Long usuarioId) {
        // A query do dashboard filtra só por usuário (sem organização): restringe ao próprio usuário do token,
        // o que também torna a chave de cache (#usuarioId) a do usuário validado.
        if (!usuarioId.equals(tenantHelper.getCurrentUserId())) {
            throw new TenantAccessDeniedException("Acesso negado: dashboard de outro usuário");
        }
        return ResponseEntity.ok(consultaService.getEstatisticasDashboardProfissional(usuarioId));
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - SUPER_ADMIN (global)
    // ===============================================================

    /**
     * Endpoint único que retorna todas as estatísticas do dashboard para SuperAdmin.
     * Sem filtro de organização — cobre todas as consultas do sistema.
     *
     * @return DTO com consultasHoje, consultasAguardando, consultasAtendidas,
     *         consultasSemana, canceladosSemana e confirmadosSemana
     */
    @GetMapping("/estatisticas/dashboard/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardSuperAdmin() {
        return ResponseEntity.ok(consultaService.getEstatisticasDashboardSuperAdmin());
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-hoje")
    public ResponseEntity<Long> getEstatisticaConsultasHojeByAdmiOrg(@PathVariable Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);
        return ResponseEntity.ok(consultaService.getEstatisticaConsultasHojeByAdmiOrg(organizacaoId));
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-atendidas-hoje")
    public ResponseEntity<Long> getEstatisticaConsultasAtendidasByAdmiOrg(@PathVariable Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);
        return ResponseEntity.ok(consultaService.getEstatisticaConsultasAtendidasByAdmiOrg(organizacaoId));
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-agendadas-hoje")
    public ResponseEntity<Long> getEstatisticasConsultaAgendadasHojeByOrd(@PathVariable Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);
        return ResponseEntity.ok(consultaService.getEstatisticasConsultaAgendadasHojeByOrd(organizacaoId));
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-semana")
    public ResponseEntity<Long> getEstatisticasSemanaPorOrganizacao(
            @PathVariable Long organizacaoId) {
        tenantHelper.validateTenant(organizacaoId);


        Long quantidade = consultaService.getEstatisticasSemanaPorOrganizacao(organizacaoId);

        return ResponseEntity.ok(quantidade);
    }

    //==================================FIM=============================================

    @GetMapping("/organizacao/{organizacaoId}/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarPorOrganizacaoEIntervalo(
            @PathVariable Long organizacaoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        tenantHelper.validateTenant(organizacaoId);
        List<ConsultaResponse> response = consultaService.buscarConsultasPorOrganizacaoEIntervalo(
                organizacaoId, dataInicial, dataFinal).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // ESTATÍSTICAS POR PROFISSIONAL (usuarioId + orgId) - HOJE
    // ==========================================

    /**
     * Conta todas as consultas de hoje do profissional logado
     * Filtra por usuarioId (via JOIN com profissional) + organizacaoId do TenantContext
     *
     * @param usuarioId ID do usuário logado (profissional)
     * @return Quantidade de consultas hoje
     */
    @GetMapping("/estatisticas/profissional/{usuarioId}/consultas-hoje")
    public ResponseEntity<Long> contarConsultasHojePorProfissional(@PathVariable Long usuarioId) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(consultaService.contarConsultasHojePorUsuarioEOrg(organizacaoId, usuarioId));
    }

    /**
     * Conta consultas REALIZADAS hoje do profissional logado
     * Filtra por usuarioId (via JOIN com profissional) + organizacaoId do TenantContext
     *
     * @param usuarioId ID do usuário logado (profissional)
     * @return Quantidade de consultas realizadas hoje
     */
    @GetMapping("/estatisticas/profissional/{usuarioId}/consultas-realizadas-hoje")
    public ResponseEntity<Long> contarConsultasRealizadasHojePorProfissional(@PathVariable Long usuarioId) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(consultaService.contarConsultasRealizadasHojePorUsuarioEOrg(organizacaoId, usuarioId));
    }

    /**
     * Conta consultas AGENDADAS hoje do profissional logado
     * Filtra por usuarioId (via JOIN com profissional) + organizacaoId do TenantContext
     *
     * @param usuarioId ID do usuário logado (profissional)
     * @return Quantidade de consultas agendadas hoje
     */
    @GetMapping("/estatisticas/profissional/{usuarioId}/consultas-agendadas-hoje")
    public ResponseEntity<Long> contarConsultasAgendadasHojePorProfissional(@PathVariable Long usuarioId) {
        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(consultaService.contarConsultasAgendadasHojePorUsuarioEOrg(organizacaoId, usuarioId));
    }

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    @GetMapping("/estatisticas/consultas-hoje")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> contarTodasConsultasHoje() {
        return ResponseEntity.ok(consultaService.contarTodasConsultasHoje());
    }

    @GetMapping("/estatisticas/consultas-realizadas-hoje")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> contarTodasRealizadasHoje() {
        return ResponseEntity.ok(consultaService.contarTodasConsultasRealizadasHoje());
    }

    @GetMapping("/estatisticas/consultas-agendadas-hoje")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> contarTodasAgendadasHoje() {
        return ResponseEntity.ok(consultaService.contarTodasConsultasAgendadasHoje());
    }

    // ==========================================
    // ESTATÍSTICAS POR MÉDICO E INTERVALO
    // ==========================================

    /**
     * Busca estatísticas de consultas por médico e intervalo de datas
     * Retorna contagem de consultas agrupadas por status
     * 
     * Exemplo de resposta:
     * [
     *   [0, 5],  // 5 consultas AGENDADAS
     *   [1, 3],  // 3 consultas REALIZADAS
     *   [2, 1]   // 1 consulta CANCELADA
     * ]
     * 
     * @param medicoId ID do usuário do médico (será usado para buscar o profissional via JOIN)
     * @param dataInicial Data inicial do período (formato: yyyy-MM-dd)
     * @param dataFinal Data final do período (formato: yyyy-MM-dd)
     * @return Lista de arrays com [statusOrdinal, quantidade]
     */
    @GetMapping("/estatisticas/medico/{medicoId}")
    public ResponseEntity<List<Object[]>> buscarEstatisticasPorMedicoEIntervalo(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        
        
        Long organizacaoId = TenantContext.getCurrentTenant();
        
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        
        List<Object[]> estatisticas = consultaService.buscarEstatisticasPorMedicoEIntervalo(
            organizacaoId, 
            medicoId, 
            dataInicial, 
            dataFinal
        );
        
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * Conta consultas da semana atual para o profissional logado (filtra por orgId + medicoId)
     * Se medicoId for fornecido, conta apenas consultas desse médico
     * Caso contrário, conta todas as consultas da organização
     *
     * @param medicoId ID do médico/profissional (opcional)
     * @return Quantidade de consultas da semana
     */
    @GetMapping("/estatisticas/consultas-semana")
    public ResponseEntity<Long> contarConsultasSemana(
            @RequestParam(required = false) Long medicoId) {


        Long organizacaoId = TenantContext.getCurrentTenant();

        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }

        Long quantidade = consultaService.contarConsultasSemana(organizacaoId, medicoId);

        return ResponseEntity.ok(quantidade);
    }

    /**
     * Conta consultas da semana atual de uma organização específica (AdminOrg)
     * Retorna total de consultas da organização na semana de segunda a domingo
     *
     * @param organizacaoId ID da organização
     * @return Quantidade de consultas da semana
     */

    /**
     * Conta consultas da semana atual de todas as organizações (SuperAdmin)
     * Retorna total global de consultas na semana de segunda a domingo
     *
     * @return Quantidade total de consultas da semana
     */
    @GetMapping("/estatisticas/consultas-semana-global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> contarConsultasSemanaGlobal() {


        Long quantidade = consultaService.contarConsultasSemanaGlobal();

        return ResponseEntity.ok(quantidade);
    }

    /**
     * Busca durações de consultas realizadas por médico e intervalo
     * Retorna lista bruta de durações para cálculo no frontend
     * 
     * Exemplo de resposta: [30, 45, 25, 60, 35, 40, 50]
     * 
     * @param medicoId ID do usuário do médico (será usado para buscar o profissional via JOIN)
     * @param dataInicial Data inicial do período (formato: yyyy-MM-dd)
     * @param dataFinal Data final do período (formato: yyyy-MM-dd)
     * @return Lista de durações em minutos
     */
    @GetMapping("/estatisticas/medico/{medicoId}/duracoes")
    public ResponseEntity<List<Integer>> buscarDuracoesConsultas(
            @PathVariable Long medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        
        
        Long organizacaoId = TenantContext.getCurrentTenant();
        
        if (organizacaoId == null) {
            log.warn("consulta.estatisticas negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }
        
        List<Integer> duracoes = consultaService.buscarDuracoesConsultas(
            organizacaoId, 
            medicoId, 
            dataInicial, 
            dataFinal
        );
        
        return ResponseEntity.ok(duracoes);
    }

    // ========== ENDPOINT PARA BUSCAR HORÁRIOS OCUPADOS ==========

    /**
     * Busca histórico completo de consultas de um paciente, aplicando as
     * regras de acesso conforme o perfil do usuário autenticado (token JWT):
     * - Administrador (GESTOR/ROOT): vê o histórico completo do paciente,
     *   combinando prontuários médicos e odontológicos de todos os profissionais.
     * - Clínico (Médico ou Dentista): vê apenas os registros dos quais ele próprio é o autor.
     *
     * @param pacienteId ID do paciente
     * @param usuarioLogado Usuário autenticado, extraído do token JWT
     * @return Lista unificada com histórico completo
     */
    @GetMapping("/BuscandoHistoricoDeConsultasDoPaciente/{pacienteId}")
    public ResponseEntity<List<HistoricoConsultaPacienteResponse>> buscarHistoricoCompletoPaciente(
            @PathVariable Long pacienteId,
            @AuthenticationPrincipal Usuario usuarioLogado) {


        List<HistoricoConsultaPacienteResponse> historico;

        if (usuarioLogado.isClinico()) {
            Optional<Profissional> profissionalLogado = profissionalRepository.findByUsuarioIdWithRelations(usuarioLogado.getId());

            if (profissionalLogado.isEmpty()) {
                log.warn("consulta.historico negado motivo=PROFISSIONAL_NAO_ENCONTRADO usuarioId={}", usuarioLogado.getId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Profissional profissional = profissionalLogado.get();
            Long profissionalId = profissional.getId();

            if (profissional.getTipoProfissional() != null && profissional.getTipoProfissional().isDentista()) {
                historico = buscarHistoricoCompletoPacienteDentistaUseCase.executar(pacienteId, profissionalId);
            } else {
                historico = buscarHistoricoCompletoPacienteMedicoUseCase.executar(pacienteId, profissionalId);
            }
        } else {
            historico = buscarHistoricoCompletoPacienteAdminUseCase.executar(pacienteId);
        }

        return ResponseEntity.ok(historico);
    }

    // ==========================================
    // SALDO FINANCEIRO (DASHBOARD)
    // ==========================================

    /**
     * Retorna estatísticas financeiras: consultas realizadas + procedimentos terapêuticos.
     *
     * @param inicio    data de início (ISO: yyyy-MM-dd)
     * @param fim       data de fim (ISO: yyyy-MM-dd)
     * @param agruparPor "mes" (padrão) ou "semana"
     * @return SaldoFinanceiroResponse com totais e detalhamento
     */
    @GetMapping("/estatisticas/saldo-financeiro")
    public ResponseEntity<SaldoFinanceiroResponse> getSaldoFinanceiro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "mes") String agruparPor) {

        Long organizacaoId = TenantContext.getCurrentTenant();
        if (organizacaoId == null) {
            log.warn("consulta.saldoFinanceiro negado motivo=SEM_ORGANIZACAO");
            return ResponseEntity.badRequest().build();
        }


        SaldoFinanceiroResponse response = saldoFinanceiroService
                .calcularSaldo(organizacaoId, inicio, fim, agruparPor);
        return ResponseEntity.ok(response);
    }
}
