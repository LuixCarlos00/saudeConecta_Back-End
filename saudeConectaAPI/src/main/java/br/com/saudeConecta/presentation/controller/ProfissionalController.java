package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.ProfissionalService;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarProfissionalRequest;
import br.com.saudeConecta.presentation.dto.profissional.ProfissionalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {
    
    private final ProfissionalService profissionalService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ProfissionalResponse>> listarTodos() {
        List<Profissional> profissionais = profissionalService.buscarTodos();
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/paginado")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Page<ProfissionalResponse>> listarTodosPaginado(Pageable pageable) {
        Page<Profissional> page = profissionalService.buscarTodos(pageable);
        Page<ProfissionalResponse> response = page.map(ProfissionalResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'PROFISSIONAL')")
    public ResponseEntity<ProfissionalResponse> buscarPorId(@PathVariable Long id) {
        return profissionalService.buscarPorId(id)
            .map(ProfissionalResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/medicos")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ProfissionalResponse>> listarMedicos() {
        List<Profissional> medicos = profissionalService.buscarMedicos();
        List<ProfissionalResponse> response = medicos.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/dentistas")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ProfissionalResponse>> listarDentistas() {
        List<Profissional> dentistas = profissionalService.buscarDentistas();
        List<ProfissionalResponse> response = dentistas.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/tipo/{tipoCodigo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ProfissionalResponse>> listarPorTipo(@PathVariable String tipoCodigo) {
        List<Profissional> profissionais = profissionalService.buscarPorTipo(tipoCodigo.toUpperCase());
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ProfissionalResponse>> buscarPorNome(@RequestParam String nome) {
        List<Profissional> profissionais = profissionalService.buscarPorNome(nome);
        List<ProfissionalResponse> response = profissionais.stream()
            .map(ProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProfissionalResponse> cadastrar(
            @Valid @RequestBody CadastrarProfissionalRequest request) {
        Profissional profissional = profissionalService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProfissionalResponse.fromEntity(profissional));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        profissionalService.deletar(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Long> contarAtivos() {
        return ResponseEntity.ok(profissionalService.contarAtivos());
    }
}
