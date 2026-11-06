package br.com.saudeConecta.presentation.controller.plataforma;

import br.com.saudeConecta.application.plataforma.PlataformaDashboardService;
import br.com.saudeConecta.application.plataforma.dto.DashboardPlataformaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard da plataforma, só leitura e só para o SUPER_ADMIN (dono da plataforma, sem tenant).
 */
@RestController
@RequestMapping("/plataforma")
@RequiredArgsConstructor
public class PlataformaDashboardController {

    private final PlataformaDashboardService plataformaDashboardService;

    /**
     * Painel agregado da plataforma: clínicas, receita, cobrança e uso.
     *
     * @param meses janela das séries mensais (1 a 24, padrão 12); fora disso responde 422
     * @return painel agregado
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DashboardPlataformaResponse> dashboard(
            @RequestParam(name = "meses", defaultValue = "12") int meses) {
        return ResponseEntity.ok(plataformaDashboardService.gerar(meses));
    }
}
