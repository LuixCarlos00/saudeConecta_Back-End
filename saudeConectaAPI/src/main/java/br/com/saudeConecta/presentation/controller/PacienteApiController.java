package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.PacienteService;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.presentation.dto.paciente.AtualizarPacienteRequest;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteCompletoRequest;
import br.com.saudeConecta.presentation.dto.paciente.PacienteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/pacientes")
@RequiredArgsConstructor
@Slf4j
public class PacienteApiController {

    private final PacienteService pacienteService;

    @GetMapping
    public ResponseEntity<List<PacienteResponse>> listarTodos() {
        log.debug("Listando todos os pacientes do tenant");
        List<PacienteResponse> pacientes = pacienteService.buscarTodosPorTenant()
            .stream()
            .map(PacienteResponse::new)
            .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/pagina")
    public ResponseEntity<Page<PacienteResponse>> listarPaginado(
            @PageableDefault(size = 12, sort = {"paciNome"}) Pageable pageable) {
        log.debug("Listando pacientes paginados do tenant");
        Page<PacienteResponse> pacientes = pacienteService.buscarTodosPorTenant(pageable)
            .map(PacienteResponse::new);
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando paciente por ID: {} no tenant", id);
        return pacienteService.buscarPorIdTenant(id)
            .map(p -> ResponseEntity.ok(new PacienteResponse(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<PacienteResponse>> buscarPorNome(@RequestParam String nome) {
        log.debug("Buscando pacientes por nome: {} no tenant", nome);
        List<PacienteResponse> pacientes = pacienteService.buscarPorNomeTenant(nome)
            .stream()
            .map(PacienteResponse::new)
            .toList();
        return ResponseEntity.ok(pacientes);
    }

    @GetMapping("/estatisticas/ativos")
    public ResponseEntity<Long> contarAtivos() {
        log.debug("Contando pacientes ativos no tenant");
        return ResponseEntity.ok(pacienteService.contarAtivosTenant());
    }

    @GetMapping("/verificar-cpf")
    public ResponseEntity<Boolean> verificarCpfExiste(@RequestParam String cpf) {
        log.debug("Verificando se CPF existe no tenant: {}", cpf);
        return ResponseEntity.ok(pacienteService.existeCpfNoTenant(cpf));
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> cadastrar(
            @Valid @RequestBody CadastrarPacienteCompletoRequest request) {
        log.info("Cadastrando paciente no tenant: {}", request.paciNome());
        Paciente paciente = pacienteService.cadastrarCompleto(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new PacienteResponse(paciente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarPacienteRequest request) {
        log.info("Atualizando paciente ID: {} no tenant", id);
        
        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarPorIdTenant(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Paciente paciente = pacienteService.atualizar(id, request);
        return ResponseEntity.ok(new PacienteResponse(paciente));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> alterarStatus(
            @PathVariable Long id,
            @RequestParam int status) {
        log.info("Alterando status do paciente ID: {} para {}", id, status);
        
        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarPorIdTenant(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        pacienteService.bloquear(id, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        log.info("Deletando paciente ID: {} do tenant", id);
        
        // Verifica se paciente pertence ao tenant
        if (pacienteService.buscarPorIdTenant(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            pacienteService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar paciente: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
