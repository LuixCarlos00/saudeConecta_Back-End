package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.planos.TipoPlano;
import br.com.saudeConecta.presentation.dto.planos.PlanoAssinaturaRequest;
import br.com.saudeConecta.presentation.dto.planos.PlanoAssinaturaResponse;
import br.com.saudeConecta.service.PlanoAssinaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PlanoAssinaturaController {

    private final PlanoAssinaturaService planoAssinaturaService;

    /**
     * Lista todos os planos ativos.
     * Acesso: público (para tela de escolha de plano)
     */
    @GetMapping
    public ResponseEntity<List<PlanoAssinaturaResponse>> listarPlanosAtivos() {
        return ResponseEntity.ok(planoAssinaturaService.listarPlanosAtivos());
    }

    /**
     * Busca um plano por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanoAssinaturaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(planoAssinaturaService.buscarPorId(id));
    }

    /**
     * Busca um plano por tipo.
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<PlanoAssinaturaResponse> buscarPorTipo(@PathVariable TipoPlano tipo) {
        return ResponseEntity.ok(planoAssinaturaService.buscarPorTipo(tipo));
    }

    /**
     * Atualiza um plano existente (SUPER_ADMIN).
     * Apenas os 3 planos fixos (STARTER, PROFISSIONAL, BUSINESS) podem ser editados.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanoAssinaturaResponse> atualizarPlano(
            @PathVariable Long id,
            @RequestBody @Valid PlanoAssinaturaRequest request) {
        log.info("Atualizando plano ID: {}", id);
        return ResponseEntity.ok(planoAssinaturaService.atualizarPlano(id, request));
    }

    /**
     * Desativa um plano (soft delete) (SUPER_ADMIN).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativarPlano(@PathVariable Long id) {
        log.info("Desativando plano ID: {}", id);
        planoAssinaturaService.desativarPlano(id);
        return ResponseEntity.noContent().build();
    }
}
