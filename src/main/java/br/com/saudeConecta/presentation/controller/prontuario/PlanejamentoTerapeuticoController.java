package br.com.saudeConecta.presentation.controller.prontuario;

import br.com.saudeConecta.application.prontuario.PlanejamentoTerapeuticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para gerenciar planejamento terapêutico do prontuário odontológico.
 */
@Slf4j
@RestController
@RequestMapping("/planejamento-terapeutico")
@RequiredArgsConstructor
public class PlanejamentoTerapeuticoController {

    private final PlanejamentoTerapeuticoService planejamentoService;

    /**
     * Remove um item do planejamento.
     * DELETE /planejamento-terapeutico/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        planejamentoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gera link de assinatura para todos os itens pendentes do prontuário.
     * POST /planejamento-terapeutico/gerar-link/{prontuarioId}?tipo=DENTISTA|MEDICO
     *
     * @param prontuarioId ID do prontuário
     * @param tipo tipo do profissional ("DENTISTA" ou "MEDICO")
     */
    @PostMapping("/gerar-link/{prontuarioId}")
    public ResponseEntity<Map<String, String>> gerarLink(
            @PathVariable Long prontuarioId,
            @RequestParam(defaultValue = "DENTISTA") String tipo) {

        String token = planejamentoService.gerarLinkAssinatura(prontuarioId, tipo);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }
}
