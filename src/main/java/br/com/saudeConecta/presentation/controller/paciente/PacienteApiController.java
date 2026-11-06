package br.com.saudeConecta.presentation.controller.paciente;

import br.com.saudeConecta.application.paciente.PacienteService;
import br.com.saudeConecta.application.paciente.dto.AtualizarPacienteRequest;
import br.com.saudeConecta.application.paciente.dto.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.application.paciente.dto.PacienteResponse;
import br.com.saudeConecta.application.usuario.dto.BloquearUsuarioRequest;
import br.com.saudeConecta.domain.paciente.Paciente;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Slf4j
public class PacienteApiController {

    private final PacienteService pacienteService;

    @PostMapping("/cadastrarPacientebyOrg")
    public ResponseEntity<PacienteResponse> cadastrarPacientebyOrg(
            @Valid @RequestBody CadastrarPacienteCompletoRequest request) {
        // Conflito (IllegalStateException) vira 409 e erro inesperado vira 500 no GlobalExceptionHandler
        Paciente paciente = pacienteService.cadastrarPacientebyOrg(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PacienteResponse(paciente));
    }

    @GetMapping("/buscarrPacientebyOrg/{id}")
    public ResponseEntity<PacienteResponse> buscarrPacientebyOrg(@PathVariable Long id) {
        return pacienteService.buscarrPacientebyOrg(id)
                .map(p -> ResponseEntity.ok(new PacienteResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/atualizarPacientebyOrg/{id}")
    public ResponseEntity<PacienteResponse> atualizarPacientebyOrg(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarPacienteRequest request) {

        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarrPacientebyOrg(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Paciente paciente = pacienteService.atualizarPacientebyOrg(id, request);
        return ResponseEntity.ok(new PacienteResponse(paciente));
    }

    @DeleteMapping("/deletarPacientebyOrg/{id}")
    public ResponseEntity<Void> deletarPacientebyOrg(@PathVariable Long id) {

        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarrPacientebyOrg(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        pacienteService.deletarPacientebyOrg(id);
        return ResponseEntity.noContent().build();
    }

    // ==========CRUD============================

// ============================= Pesquisas com filtros  ========================================

    // Métodos de busca para autocomplete com filtro opcional
    @GetMapping("/buscar-por-nome")
    public ResponseEntity<List<PacienteResponse>> buscarListaPacientesPorNome(
            @RequestParam String nome,
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<PacienteResponse> pacientes = pacienteService.buscarListaPacientesPorNomeComFiltro(nome, filtro)
                .stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscar-por-cpf")
    public ResponseEntity<List<PacienteResponse>> buscarListaPacientesPorCPF(
            @RequestParam String cpf,
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<PacienteResponse> pacientes = pacienteService.buscarListaPacientesPorCPFComFiltro(cpf, filtro)
                .stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscar-por-rg")
    public ResponseEntity<List<PacienteResponse>> buscarListaPacientesPorRG(
            @RequestParam String rg,
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<PacienteResponse> pacientes = pacienteService.buscarListaPacientesPorRGComFiltro(rg, filtro)
                .stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscar-por-telefone")
    public ResponseEntity<List<PacienteResponse>> buscarListaPacientesPorTelefone(
            @RequestParam String telefone,
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<PacienteResponse> pacientes = pacienteService.buscarListaPacientesPorTelefoneComFiltro(telefone, filtro)
                .stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/buscar-todos")
    public ResponseEntity<List<PacienteResponse>> buscarTodosPacientes(
            @RequestParam(required = false, defaultValue = "ALL") String filtro) {
        List<PacienteResponse> pacientes = pacienteService.buscarTodosPacientesComFiltro(filtro)
                .stream()
                .map(PacienteResponse::new)
                .toList();
        return ResponseEntity.ok(pacientes);
    }

// ============================= Pesquisas com filtros  ========================================

    @PutMapping("/bloquearPacientebyOrg")
    public ResponseEntity<Void> bloquearPacientebyOrg(
            @RequestBody @Valid @NotNull BloquearUsuarioRequest request) {

        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarrPacientebyOrg(request.codigo()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        pacienteService.bloquearPacientebyOrg(request.codigo(), request.status());
        return ResponseEntity.ok().build();
    }

}
