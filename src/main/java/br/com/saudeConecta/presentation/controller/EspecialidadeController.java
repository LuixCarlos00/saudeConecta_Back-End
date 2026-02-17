package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.EspecialidadeService;
import br.com.saudeConecta.presentation.dto.especialidade.EspecialidadeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/especialidades")
@RequiredArgsConstructor
public class EspecialidadeController {
    
    private final EspecialidadeService service;
    
    @GetMapping
    public ResponseEntity<List<EspecialidadeResponse>> listarTodas() {
        List<EspecialidadeResponse> response = service.listarAtivas().stream()
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
}
