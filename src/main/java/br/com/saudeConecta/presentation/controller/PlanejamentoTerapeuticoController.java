package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.prontuario.PlanejamentoTerapeuticoService;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private List<Map<String, Object>> converterParaResponse(List<PlanejamentoTerapeutico> lista) {
        return lista.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("dataProcedimento", p.getDataProcedimento());
            item.put("procedimentoRealizado", p.getProcedimentoRealizado());
            item.put("valor", p.getValor());
            item.put("statusAssinatura", p.getStatusAssinatura());
            item.put("dataAssinatura", p.getDataAssinatura());
            item.put("assinaturaBase64", p.getAssinaturaBase64());
            return item;
        }).collect(Collectors.toList());
    }
}
