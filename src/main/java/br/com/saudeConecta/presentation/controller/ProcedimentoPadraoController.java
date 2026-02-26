package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.prontuario.ProcedimentoPadrao;
import br.com.saudeConecta.presentation.dto.prontuario.ProcedimentoPadraoRequest;
import br.com.saudeConecta.service.ProcedimentoPadraoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller para gerenciar procedimentos padrão do profissional.
 */
@Slf4j
@RestController
@RequestMapping("/procedimentos-padrao")
@RequiredArgsConstructor
public class ProcedimentoPadraoController {

    private final ProcedimentoPadraoService procedimentoService;

    /**
     * Lista procedimentos ativos do profissional.
     * GET /procedimentos-padrao/profissional/{profissionalId}
     */
    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<Map<String, Object>>> listar(@PathVariable Long profissionalId) {
        log.info("GET /procedimentos-padrao/profissional/{}", profissionalId);

        List<ProcedimentoPadrao> lista = procedimentoService.listarPorProfissional(profissionalId);

        List<Map<String, Object>> response = lista.stream().map(p -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", p.getId());
            item.put("nomeProcedimento", p.getNomeProcedimento());
            item.put("valorPadrao", p.getValorPadrao());
            item.put("ativo", p.getAtivo());
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Cria um novo procedimento padrão.
     * POST /procedimentos-padrao/profissional/{profissionalId}
     */
    @PostMapping("/profissional/{profissionalId}")
    public ResponseEntity<Map<String, Object>> criar(
            @PathVariable Long profissionalId,
            @RequestBody ProcedimentoPadraoRequest request) {

        log.info("POST /procedimentos-padrao/profissional/{} — nome={}", profissionalId, request.getNomeProcedimento());

        ProcedimentoPadrao criado = procedimentoService.criar(profissionalId, request);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", criado.getId());
        response.put("nomeProcedimento", criado.getNomeProcedimento());
        response.put("valorPadrao", criado.getValorPadrao());
        response.put("ativo", criado.getAtivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Atualiza um procedimento padrão.
     * PUT /procedimentos-padrao/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizar(
            @PathVariable Long id,
            @RequestBody ProcedimentoPadraoRequest request) {

        log.info("PUT /procedimentos-padrao/{}", id);

        ProcedimentoPadrao atualizado = procedimentoService.atualizar(id, request);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", atualizado.getId());
        response.put("nomeProcedimento", atualizado.getNomeProcedimento());
        response.put("valorPadrao", atualizado.getValorPadrao());

        return ResponseEntity.ok(response);
    }

    /**
     * Desativa um procedimento (soft delete).
     * DELETE /procedimentos-padrao/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        log.info("DELETE /procedimentos-padrao/{}", id);
        procedimentoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
