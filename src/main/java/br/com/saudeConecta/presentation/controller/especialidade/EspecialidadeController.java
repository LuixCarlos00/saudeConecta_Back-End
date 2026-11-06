package br.com.saudeConecta.presentation.controller.especialidade;

import br.com.saudeConecta.application.especialidade.EspecialidadeService;
import br.com.saudeConecta.application.especialidade.dto.EspecialidadeResponse;
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
