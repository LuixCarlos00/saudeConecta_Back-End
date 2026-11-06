package br.com.saudeConecta.presentation.controller.assinatura;

import br.com.saudeConecta.application.assinatura.PlanoAssinaturaService;
import br.com.saudeConecta.application.assinatura.dto.PlanoAssinaturaRequest;
import br.com.saudeConecta.application.assinatura.dto.PlanoAssinaturaResponse;
import br.com.saudeConecta.domain.planos.TipoPlano;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planos")
@RequiredArgsConstructor
@Slf4j
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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlanoAssinaturaResponse> atualizarPlano(
            @PathVariable Long id,
            @RequestBody @Valid PlanoAssinaturaRequest request) {
        return ResponseEntity.ok(planoAssinaturaService.atualizarPlano(id, request));
    }

    /**
     * Desativa um plano (soft delete) (SUPER_ADMIN).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> desativarPlano(@PathVariable Long id) {
        planoAssinaturaService.desativarPlano(id);
        return ResponseEntity.noContent().build();
    }
}
