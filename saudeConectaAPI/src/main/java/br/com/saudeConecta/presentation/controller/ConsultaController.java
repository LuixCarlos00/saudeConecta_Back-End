package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.ConsultaService;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.AtualizarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.CancelarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.ConsultaResponse;
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


    @GetMapping("/profissional/{profissionalId}/intervalo")
    public ResponseEntity<List<ConsultaResponse>> pesquisarClinicasEmIntervaloDeDatas(
            @PathVariable Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status) {
        log.debug("- Iniciando pesquisa de clínicas em intervalo de datas - pesquisarClinicasEmIntervaloDeDatas" );
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

    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-hoje")
    public ResponseEntity<Long> contarConsultasHojePorOrganizacao(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(consultaService.contarConsultasHojePorOrganizacao(organizacaoId));
    }


    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-realizadas-hoje")
    public ResponseEntity<Long> contarRealizadasHojePorOrganizacao(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(consultaService.contarConsultasRealizadasHojePorOrganizacao(organizacaoId));
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/consultas-agendadas-hoje")
    public ResponseEntity<Long> contarAgendadasHojePorOrganizacao(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(consultaService.contarConsultasAgendadasHojePorOrganizacao(organizacaoId));
    }

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


    // ========== ENDPOINT PARA BUSCAR HORÁRIOS OCUPADOS ==========

}
