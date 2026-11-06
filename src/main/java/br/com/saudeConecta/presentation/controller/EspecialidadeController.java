package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.service.EspecialidadeService;
import br.com.saudeConecta.presentation.dto.especialidade.EspecialidadeResponse;
import lombok.RequiredArgsConstructor;
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

}
