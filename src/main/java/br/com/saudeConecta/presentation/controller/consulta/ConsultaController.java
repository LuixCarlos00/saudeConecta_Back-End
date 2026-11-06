package br.com.saudeConecta.presentation.controller.consulta;

import br.com.saudeConecta.application.consulta.ConsultaAgendamentoService;
import br.com.saudeConecta.application.consulta.ConsultaBuscaService;
import br.com.saudeConecta.application.consulta.dto.*;
import br.com.saudeConecta.application.historico.BuscarHistoricoPacientePorPerfilUseCase;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.common.Periodo;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.domain.usuario.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import br.com.saudeConecta.config.logging.MascaraLog;
import java.util.List;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
@Slf4j
public class ConsultaController {
    
    private final ConsultaAgendamentoService consultaAgendamentoService;
    private final ConsultaBuscaService consultaBuscaService;
    private final BuscarHistoricoPacientePorPerfilUseCase buscarHistoricoPacientePorPerfilUseCase;
    private final TenantHelper tenantHelper;

//=================Tela de /gerenciamento =================
    @GetMapping("/hoje")
    public ResponseEntity<List<ConsultaResponse>> listarConsultasHoje(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        List<ConsultaResponse> response = consultaBuscaService.buscarConsultasHoje(data, usuarioLogado).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/semana-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDaSemanaAtual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        List<ConsultaResponse> response =
                consultaBuscaService.buscarConsultasDaSemanaAtual(data, usuarioLogado).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/mes-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDoMesAtual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        List<ConsultaResponse> response =
                consultaBuscaService.buscarConsultasDoMesAtual(data, usuarioLogado).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ano-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDoAnoAtual() {
        List<ConsultaResponse> response =
                consultaBuscaService.buscarConsultasDoAnoAtual().stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    //=========================================Tela de /gerenciamento =========================================
//=================================================CRUD =========================================

    @PostMapping("/cadastrarConsultaByOrg")
    public ResponseEntity<ConsultaResponse> cadastrarConsultaByOrg(@Valid @RequestBody AgendarConsultaRequest request) {
        Consulta consulta = consultaAgendamentoService.cadastrarConsultaByOrg(request);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    @PutMapping("/atualizarConsultabyOrg/{id}")
    public ResponseEntity<ConsultaResponse> atualizarConsultabyOrg(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConsultaRequest request) {
        Consulta consulta = consultaAgendamentoService.atualizarConsultaByOrg(id, request);
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
    public ResponseEntity<Void> deletarConsulta(@PathVariable Long id) {
        // Exclusão bloqueada (IllegalStateException) vira 409 com mensagem no GlobalExceptionHandler
        try {
            consultaAgendamentoService.deletarConsulta(id);
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
     * IMPORTANTE: Status REALIZADA só é definido quando o profissional finaliza o prontuário (médico ou odontológico)
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
            Consulta consulta = consultaAgendamentoService.atualizarStatus(id, novoStatus, motivo);
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
        List<String> horariosOcupados = consultaAgendamentoService.buscarHorariosOcupados(medicoId, data);
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
        return ResponseEntity.ok(consultaAgendamentoService.buscarHorariosOcupadosDetalhado(medicoId, data));
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
        boolean existeConsulta = consultaAgendamentoService.verificarDisponibilidade(data, horario, medicoId);
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
        LocalDateTime inicio = dataInicial != null ? Periodo.dia(dataInicial).inicio() : null;
        LocalDateTime fim = dataFinal != null ? Periodo.dia(dataFinal).fim() : null;
        
        // Converter string de status para lista de StatusConsulta
        List<StatusConsulta> statusList = null;
        if (status != null && !status.trim().isEmpty()) {
            statusList = java.util.Arrays.stream(status.split(","))
                    .map(String::trim)
                    .map(StatusConsulta::valueOf)
                    .toList();
        }
        
        List<ConsultaResponse> response = consultaBuscaService.buscarComFiltrosDinamicos(
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
        List<ConsultaResponse> response = consultaBuscaService.buscarConsultasPorIntervalo(dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

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
        List<ConsultaResponse> response = consultaBuscaService.pesquisarClinicasEmIntervaloDeDatas(
                        profissionalId, dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

//============================================================

    @GetMapping("/{id}")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        return consultaBuscaService.buscarPorIdComRelacoes(id)
            .map(ConsultaResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
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
        List<ConsultaResponse> response = consultaBuscaService.buscarConsultasPorMedicoEPeriodo(usuarioId, tipoPeriodo)
                .stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizacao/{organizacaoId}/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarPorOrganizacaoEIntervalo(
            @PathVariable Long organizacaoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        tenantHelper.validateTenant(organizacaoId);
        List<ConsultaResponse> response = consultaBuscaService.buscarConsultasPorOrganizacaoEIntervalo(
                organizacaoId, dataInicial, dataFinal).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

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

        return buscarHistoricoPacientePorPerfilUseCase.executar(pacienteId, usuarioLogado)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
