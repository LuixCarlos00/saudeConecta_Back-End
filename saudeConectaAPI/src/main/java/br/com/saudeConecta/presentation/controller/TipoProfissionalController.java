package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.EspecialidadeService;
import br.com.saudeConecta.presentation.dto.especialidade.TipoProfissionalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/tipos-profissional")
@RequiredArgsConstructor
public class TipoProfissionalController {
    
    private final EspecialidadeService service;
    
    @GetMapping
    public ResponseEntity<List<TipoProfissionalResponse>> listarTodos() {
        List<TipoProfissionalResponse> response = service.listarTiposProfissional().stream()
            .map(TipoProfissionalResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{codigo}")
    public ResponseEntity<TipoProfissionalResponse> buscarPorCodigo(@PathVariable String codigo) {
        return service.buscarTipoPorCodigo(codigo)
            .map(TipoProfissionalResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
