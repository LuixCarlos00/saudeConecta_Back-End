package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.ConsultaService;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.consulta.StatusConsulta;
import br.com.saudeConecta.infra.tenant.TenantContext;
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.AtualizarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.CancelarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.ConsultaResponse;
import br.com.saudeConecta.presentation.dto.consulta.EstatisticasDashboardAdminOrgResponse;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaPacienteResponse;
import br.com.saudeConecta.presentation.dto.consulta.HistoricoConsultaDentistaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
@Slf4j
public class ConsultaController {
    
    private final ConsultaService consultaService;

//=================Tela de /gerenciamento =================
    @GetMapping("/hoje")
    public ResponseEntity<List<ConsultaResponse>> listarConsultasHoje() {
        log.debug("- Iniciando busca de consultas de hoje - listarConsultasHoje" );
        List<ConsultaResponse> response = consultaService.buscarConsultasHoje().stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/semana-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDaSemanaAtual() {
        log.debug("- Iniciando busca de consultas da semana atual - buscarDaSemanaAtual" );
        List<ConsultaResponse> response =
                consultaService.buscarConsultasDaSemanaAtual().stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/mes-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDoMesAtual() {
        log.debug("- Iniciando busca de consultas do mês atual - buscarDoMesAtual" );
        List<ConsultaResponse> response =
                consultaService.buscarConsultasDoMesAtual().stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ano-atual")
    public ResponseEntity<List<ConsultaResponse>> buscarDoAnoAtual() {
        log.debug("- Iniciando busca de consultas do ano atual - buscarDoAnoAtual" );
        List<ConsultaResponse> response =
                consultaService.buscarConsultasDoAnoAtual().stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }


    @PostMapping("/cadastrarConsultaByOrg")
    public ResponseEntity<ConsultaResponse> cadastrarConsultaByOrg(@Valid @RequestBody AgendarConsultaRequest request) {
        log.debug("- Iniciando cadastro de consulta - cadastrarConsultaByOrg");
        Consulta consulta = consultaService.cadastrarConsultaByOrg(request);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    @PutMapping("/atualizarConsultabyOrg/{id}")
    public ResponseEntity<ConsultaResponse> atualizarConsultabyOrg(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarConsultaRequest request) {
        log.debug("- Iniciando atualização de consulta - atualizarConsultabyOrg" );
        Consulta consulta = consultaService.atualizarConsultaByOrg(id, request);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    @PutMapping("/concluirConsultabyOrg/{id}")
    public ResponseEntity<ConsultaResponse> concluirConsultabyOrg(@PathVariable Long id) {
        log.debug("- Iniciando conclusão de consulta - concluirConsultabyOrg" );
        Consulta consulta = consultaService.concluirConsultabyOrg(id);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }

    /**
     * Altera o status de uma consulta.
     * Aceita todos os status: AGENDADA, CONFIRMADA, CANCELADA, REALIZADA, PAGO
     *
     * @param id     ID da consulta
     * @param status Novo status
     * @param motivo Motivo (obrigatório apenas para CANCELADA)
     * @return Consulta atualizada
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ConsultaResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String motivo) {
        log.info("Atualizando status da consulta {} para {}", id, status);
        try {
            StatusConsulta novoStatus = StatusConsulta.valueOf(status.toUpperCase());
            Consulta consulta = consultaService.atualizarStatus(id, novoStatus, motivo);
            return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
        } catch (IllegalArgumentException e) {
            log.warn("Status inválido ou consulta não encontrada: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("Transição de status inválida: {}", e.getMessage());
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    /**
     * Altera o status de uma consulta REALIZADA para PAGO.
     *
     * @param id ID da consulta
     * @return Consulta atualizada com status PAGO
     */
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<ConsultaResponse> marcarComoPago(@PathVariable Long id) {
        log.info("Marcando consulta {} como PAGO", id);
        try {
            Consulta consulta = consultaService.marcarComoPago(id);
            return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
        } catch (IllegalArgumentException e) {
            log.warn("Consulta não encontrada: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("Transição de status inválida para PAGO: {}", e.getMessage());
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    @GetMapping("/horarios-ocupados")
    public ResponseEntity<List<String>> buscarHorariosOcupados(
            @RequestParam Long medicoId,
            @RequestParam String data) {
        log.debug("- Iniciando busca de horários ocupados - buscarHorariosOcupados" );
        List<String> horariosOcupados = consultaService.buscarHorariosOcupados(medicoId, data);
        return ResponseEntity.ok(horariosOcupados);
    }

    @GetMapping("/verificarDisponibilidade")
    public ResponseEntity<Boolean> verificarDisponibilidade(
            @RequestParam String data,
            @RequestParam String horario,
            @RequestParam Long medicoId) {
        log.debug("- Iniciando verificação de disponibilidade -verificarDisponibilidade" );
        boolean existeConsulta = consultaService.verificarDisponibilidade(data, horario, medicoId);
        return ResponseEntity.ok(existeConsulta);
    }



    @GetMapping("/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarTodasPorIntervalo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        log.debug("- Iniciando busca de consultas por intervalo - buscarTodasPorIntervalo" );
        List<ConsultaResponse> response = consultaService.buscarConsultasPorIntervalo(dataInicial, dataFinal, status).stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }



    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<ConsultaResponse>> listarPorProfissional(@PathVariable Long profissionalId) {
        log.debug("- Iniciando busca de consultas por profissional - listarPorProfissional" );
        List<ConsultaResponse> response = consultaService.buscarPorProfissional(profissionalId).stream()
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
        log.debug("Buscando consultas para usuário {} entre {} e {} com status {}", 
                  profissionalId, dataInicial, dataFinal, status);
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
        log.debug("- Iniciando pesquisa de médico e especialidade em intervalo de datas - pesquisarMedicoEspecialidadeEmIntervaloDeDatas" );
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
        log.debug("- Iniciando pesquisa de profissional e especialidade - pesquisarPorProfissionalEEspecialidade" );
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
        log.debug("- Iniciando pesquisa de especialidade em intervalo de datas - pesquisarEspecialidadeEmIntervaloDeDatas" );
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
        log.debug("- Iniciando pesquisa de especialidade - pesquisarPorEspecialidade" );
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
        log.debug("- Iniciando pesquisa de especialidade e organização - pesquisarPorEspecialidadeEOrganizacao" );
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

    

    @PutMapping("/{id}/realizar")
    public ResponseEntity<ConsultaResponse> realizar(
            @PathVariable Long id,
            @RequestParam(required = false) String observacoes) {
        Consulta consulta = consultaService.realizar(id, observacoes);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
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
        log.debug("Buscando consultas para usuário {} com período {}", usuarioId, tipoPeriodo);
        List<ConsultaResponse> response = consultaService.buscarConsultasPorMedicoEPeriodo(usuarioId, tipoPeriodo)
                .stream()
                .map(ConsultaResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ===============================================================
    // BUSCAS DE ESTATISTICAS - Dashboard - Admin_ORGANIZACAO
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

        log.debug("Buscando estatísticas de dashboard para organização {}", organizacaoId);

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
        log.debug("Buscando estatísticas de dashboard para profissional usuarioId={}", usuarioId);
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
    public ResponseEntity<EstatisticasDashboardAdminOrgResponse> getEstatisticasDashboardSuperAdmin() {
        log.debug("Buscando estatísticas de dashboard global para SuperAdmin");
        return ResponseEntity.ok(consultaService.getEstatisticasDashboardSuperAdmin());
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-hoje")
    public ResponseEntity<Long> getEstatisticaConsultasHojeByAdmiOrg(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(consultaService.getEstatisticaConsultasHojeByAdmiOrg(organizacaoId));
    }


    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-atendidas-hoje")
    public ResponseEntity<Long> getEstatisticaConsultasAtendidasByAdmiOrg(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(consultaService.getEstatisticaConsultasAtendidasByAdmiOrg(organizacaoId));
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-agendadas-hoje")
    public ResponseEntity<Long> getEstatisticasConsultaAgendadasHojeByOrd(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(consultaService.getEstatisticasConsultaAgendadasHojeByOrd(organizacaoId));
    }


    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-semana")
    public ResponseEntity<Long> getEstatisticasSemanaPorOrganizacao(
            @PathVariable Long organizacaoId) {

        log.debug("Contando consultas da semana para organização {}", organizacaoId);

        Long quantidade = consultaService.getEstatisticasSemanaPorOrganizacao(organizacaoId);

        return ResponseEntity.ok(quantidade);
    }


    //==================================FIM=============================================


    @GetMapping("/organizacao/{organizacaoId}/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarPorOrganizacaoEIntervalo(
            @PathVariable Long organizacaoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
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
            log.error("Organização não identificada no contexto");
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
            log.error("Organização não identificada no contexto");
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
            log.error("Organização não identificada no contexto");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(consultaService.contarConsultasAgendadasHojePorUsuarioEOrg(organizacaoId, usuarioId));
    }

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    @GetMapping("/estatisticas/consultas-hoje")
    public ResponseEntity<Long> contarTodasConsultasHoje() {
        return ResponseEntity.ok(consultaService.contarTodasConsultasHoje());
    }

    @GetMapping("/estatisticas/consultas-realizadas-hoje")
    public ResponseEntity<Long> contarTodasRealizadasHoje() {
        return ResponseEntity.ok(consultaService.contarTodasConsultasRealizadasHoje());
    }

    @GetMapping("/estatisticas/consultas-agendadas-hoje")
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
        
        log.debug("Buscando estatísticas para usuário {} entre {} e {}", medicoId, dataInicial, dataFinal);
        
        Long organizacaoId = TenantContext.getCurrentTenant();
        
        if (organizacaoId == null) {
            log.error("Organização não identificada no contexto");
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

        log.debug("Contando consultas da semana - MedicoId: {}", medicoId);

        Long organizacaoId = TenantContext.getCurrentTenant();

        if (organizacaoId == null) {
            log.error("Organização não identificada no contexto");
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
    public ResponseEntity<Long> contarConsultasSemanaGlobal() {

        log.debug("Contando consultas da semana globalmente");

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
        
        log.debug("Buscando durações de consultas para usuário {} entre {} e {}", medicoId, dataInicial, dataFinal);
        
        Long organizacaoId = TenantContext.getCurrentTenant();
        
        if (organizacaoId == null) {
            log.error("Organização não identificada no contexto");
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


    @GetMapping("/BuscandoHistoricoDeConsultasDoPaciente/{pacienteId}")
    public ResponseEntity<List<HistoricoConsultaPacienteResponse>> buscarHistoricoCompletoPaciente(
            @PathVariable Long pacienteId) {
        
        log.info("=== Requisição recebida: GET /consultas/BuscandoHistoricoDeConsultasDoPaciente/{} ===", pacienteId);
        
        try {
            List<HistoricoConsultaPacienteResponse> historico = 
                consultaService.buscarHistoricoCompletoPaciente(pacienteId);
            
            log.info("Histórico de consultas retornado com sucesso - {} registros", historico.size());
            return ResponseEntity.ok(historico);
            
        } catch (Exception e) {
            log.error("Erro ao buscar histórico de consultas do paciente {}: {}", pacienteId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busca histórico completo de consultas odontológicas de um paciente
     * Retorna consultas REALIZADAS que possuem prontuário dentista
     *
     * @param pacienteId ID do paciente
     * @return Lista com histórico odontológico completo
     */
    @GetMapping("/BuscandoHistoricoDeConsultasDoPaciente_dentista/{pacienteId}")
    public ResponseEntity<List<HistoricoConsultaDentistaResponse>> buscarHistoricoCompletoPacienteDentista(
            @PathVariable Long pacienteId) {

        log.info("=== Requisição recebida: GET /consultas/BuscandoHistoricoDeConsultasDoPaciente_dentista/{} ===", pacienteId);

        try {
            List<HistoricoConsultaDentistaResponse> historico =
                consultaService.buscarHistoricoCompletoPacienteDentista(pacienteId);

            log.info("Histórico odontológico retornado com sucesso - {} registros", historico.size());
            return ResponseEntity.ok(historico);

        } catch (Exception e) {
            log.error("Erro ao buscar histórico odontológico do paciente {}: {}", pacienteId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
