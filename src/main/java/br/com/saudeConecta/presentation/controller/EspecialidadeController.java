package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.service.EspecialidadeService;
import br.com.saudeConecta.presentation.dto.especialidade.EspecialidadeRequest;
import br.com.saudeConecta.presentation.dto.especialidade.EspecialidadeResponse;
import br.com.saudeConecta.presentation.dto.especialidade.EspecialidadeUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/especialidades")
@RequiredArgsConstructor
public class EspecialidadeController {
    
    private final EspecialidadeService service;
    
    @GetMapping("/carregarEspecialidades")
    public ResponseEntity<List<EspecialidadeResponse>> carregarEspecialidades() {
        List<EspecialidadeResponse> response = service.carregarEspecialidades().stream()
            .map(EspecialidadeResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EspecialidadeResponse> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
            .map(EspecialidadeResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/medicas")
    public ResponseEntity<List<EspecialidadeResponse>> listarMedicas() {
        List<EspecialidadeResponse> response = service.listarMedicas().stream()
            .map(EspecialidadeResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/odontologicas")
    public ResponseEntity<List<EspecialidadeResponse>> listarOdontologicas() {
        List<EspecialidadeResponse> response = service.listarOdontologicas().stream()
            .map(EspecialidadeResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/tipo/{codigo}")
    public ResponseEntity<List<EspecialidadeResponse>> listarPorTipo(@PathVariable String codigo) {
        List<EspecialidadeResponse> response = service.listarPorTipo(codigo).stream()
            .map(EspecialidadeResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/tipo-id/{tipoId}")
    public ResponseEntity<List<EspecialidadeResponse>> listarPorTipoId(@PathVariable Long tipoId) {
        List<EspecialidadeResponse> response = service.listarPorTipoId(tipoId).stream()
            .map(EspecialidadeResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<EspecialidadeResponse> criar(@Valid @RequestBody EspecialidadeRequest request) {
        try {
            Especialidade especialidade = service.criar(
                request.tipoProfissionalId(),
                request.nome(),
                request.codigo()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(EspecialidadeResponse.fromEntity(especialidade));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<EspecialidadeResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadeUpdateRequest request) {
        try {
            Especialidade especialidade = service.atualizar(
                id,
                request.nome(),
                request.codigo(),
                request.status()
            );
            return ResponseEntity.ok(EspecialidadeResponse.fromEntity(especialidade));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        try {
            service.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
