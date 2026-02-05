package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.ProfissionalService;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarClinicoRequest;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarProfissionalRequest;
import br.com.saudeConecta.presentation.dto.profissional.ProfissionalResponse;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profissionais")
@RequiredArgsConstructor
@Slf4j
public class ProfissionalController {
    
    private final ProfissionalService profissionalService;
    
    @GetMapping
    public ResponseEntity<List<ProfissionalResponse>> listarTodos() {
        List<Profissional> profissionais = profissionalService.buscarTodos();
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginado")
    public ResponseEntity<Page<ProfissionalResponse>> listarTodosPaginado(Pageable pageable) {
        Page<Profissional> page = profissionalService.buscarTodos(pageable);
        Page<ProfissionalResponse> response = page.map(ProfissionalResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProfissionalResponse> buscarPorId(@PathVariable Long id) {
        return profissionalService.buscarPorId(id)
            .map(ProfissionalResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/medicos")
    public ResponseEntity<List<ProfissionalResponse>> listarMedicos() {
        List<Profissional> medicos = profissionalService.buscarMedicos();
        List<ProfissionalResponse> response = medicos.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/dentistas")
    public ResponseEntity<List<ProfissionalResponse>> listarDentistas() {
        List<Profissional> dentistas = profissionalService.buscarDentistas();
        List<ProfissionalResponse> response = dentistas.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/tipo/{tipoCodigo}")
    public ResponseEntity<List<ProfissionalResponse>> listarPorTipo(@PathVariable String tipoCodigo) {
        List<Profissional> profissionais = profissionalService.buscarPorTipo(tipoCodigo.toUpperCase());
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<List<ProfissionalResponse>> buscarPorNome(@RequestParam String nome) {
        List<Profissional> profissionais = profissionalService.buscarPorNome(nome);
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<ProfissionalResponse> cadastrar(
            @Valid @RequestBody CadastrarProfissionalRequest request) {
        Profissional profissional = profissionalService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProfissionalResponse.fromEntity(profissional));
    }

    @PostMapping("/cadastraClinicoByOrg")
    @Description("Cadastra médico/clínico com CPF como login e envia credenciais por email. Utilizado em: CadastroMedicoComponent")
    public ResponseEntity<?> cadastraClinicoByOrg(@Valid @RequestBody CadastrarClinicoRequest request) {
        log.info("Cadastrando clínico: {}", request.medNome());
        try {
            Profissional profissional = profissionalService.cadastraClinicoByOrg(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProfissionalResponse.fromEntity(profissional));
        } catch (IllegalStateException e) {
            log.warn("Erro ao cadastrar clínico: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        profissionalService.deletar(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> contarAtivos() {
        return ResponseEntity.ok(profissionalService.contarAtivos());
    }

    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    @GetMapping("/organizacao/{organizacaoId}")
    public ResponseEntity<List<ProfissionalResponse>> listarPorOrganizacao(@PathVariable Long organizacaoId) {
        List<Profissional> profissionais = profissionalService.buscarPorOrganizacao(organizacaoId);
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estatisticas/organizacao/{organizacaoId}/medicos-ativos")
    public ResponseEntity<Long> contarAtivosPorOrganizacao(@PathVariable Long organizacaoId) {
        return ResponseEntity.ok(profissionalService.contarAtivosPorOrganizacao(organizacaoId));
    }

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    @GetMapping("/estatisticas/medicos-ativos")
    public ResponseEntity<Long> contarTodosAtivos() {
        return ResponseEntity.ok(profissionalService.contarTodosAtivos());
    }

    @GetMapping("/todos-ativos")
    public ResponseEntity<List<ProfissionalResponse>> listarTodosAtivos() {
        List<Profissional> profissionais = profissionalService.buscarTodosAtivos();
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
}
