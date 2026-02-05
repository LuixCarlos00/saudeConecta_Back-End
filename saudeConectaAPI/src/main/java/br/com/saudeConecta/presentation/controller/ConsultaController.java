package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.ConsultaService;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.presentation.dto.consulta.AgendarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.CancelarConsultaRequest;
import br.com.saudeConecta.presentation.dto.consulta.ConsultaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {
    
    private final ConsultaService consultaService;
    
    @GetMapping
    public ResponseEntity<List<ConsultaResponse>> listarTodas() {
        List<ConsultaResponse> response = consultaService.buscarTodas().stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
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
    
    @GetMapping("/hoje")
    public ResponseEntity<List<ConsultaResponse>> listarConsultasHoje() {
        List<ConsultaResponse> response = consultaService.buscarConsultasHoje().stream()
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
    
    @PostMapping
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody AgendarConsultaRequest request) {
        Consulta consulta = consultaService.agendar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ConsultaResponse.fromEntity(consulta));
    }
    
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<ConsultaResponse> confirmar(@PathVariable Long id) {
        Consulta consulta = consultaService.confirmar(id);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }
    
    @PutMapping("/{id}/iniciar")
    public ResponseEntity<ConsultaResponse> iniciar(@PathVariable Long id) {
        Consulta consulta = consultaService.iniciar(id);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }
    
    @PutMapping("/{id}/realizar")
    public ResponseEntity<ConsultaResponse> realizar(
            @PathVariable Long id,
            @RequestParam(required = false) String observacoes) {
        Consulta consulta = consultaService.realizar(id, observacoes);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }
    
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<ConsultaResponse> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarConsultaRequest request) {
        Consulta consulta = consultaService.cancelar(id, request);
        return ResponseEntity.ok(ConsultaResponse.fromEntity(consulta));
    }
    
    @PutMapping("/{id}/nao-compareceu")
    public ResponseEntity<ConsultaResponse> marcarNaoCompareceu(@PathVariable Long id) {
        Consulta consulta = consultaService.marcarNaoCompareceu(id);
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

    @GetMapping("/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarTodasPorIntervalo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        List<ConsultaResponse> response = consultaService.buscarTodasConsultasPorIntervalo(dataInicial, dataFinal).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profissional/{profissionalId}/intervalo")
    public ResponseEntity<List<ConsultaResponse>> buscarPorProfissionalEIntervalo(
            @PathVariable Long profissionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal) {
        List<ConsultaResponse> response = consultaService.buscarConsultasPorProfissionalEIntervalo(
                profissionalId, dataInicial, dataFinal).stream()
            .map(ConsultaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
}
