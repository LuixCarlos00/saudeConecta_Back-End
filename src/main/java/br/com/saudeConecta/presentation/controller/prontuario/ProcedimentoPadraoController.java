package br.com.saudeConecta.presentation.controller.prontuario;

import br.com.saudeConecta.application.prontuario.ProcedimentoPadraoService;
import br.com.saudeConecta.application.prontuario.dto.ProcedimentoPadraoRequest;
import br.com.saudeConecta.domain.prontuario.ProcedimentoPadrao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar procedimentos padrão do profissional.
 */
@Slf4j
@RestController
@RequestMapping("/procedimentos-padrao")
@RequiredArgsConstructor
public class ProcedimentoPadraoController {
    private static final String CAMPO_NOME_PROCEDIMENTO = "nomeProcedimento";
    private static final String CAMPO_VALOR_PADRAO = "valorPadrao";
    private static final String CAMPO_ATIVO = "ativo";

    private final ProcedimentoPadraoService procedimentoService;

    /**
     * Lista procedimentos ativos do profissional.
     * GET /procedimentos-padrao/profissional/{profissionalId}
     */
    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<Map<String, Object>>> listar(@PathVariable Long profissionalId) {

        List<ProcedimentoPadrao> lista = procedimentoService.listarPorProfissional(profissionalId);

        List<Map<String, Object>> response = lista.stream().map(p -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", p.getId());
            item.put(CAMPO_NOME_PROCEDIMENTO, p.getNomeProcedimento());
            item.put(CAMPO_VALOR_PADRAO, p.getValorPadrao());
            item.put(CAMPO_ATIVO, p.getAtivo());
            return item;
        }).toList();

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


        ProcedimentoPadrao criado = procedimentoService.criar(profissionalId, request);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", criado.getId());
        response.put(CAMPO_NOME_PROCEDIMENTO, criado.getNomeProcedimento());
        response.put(CAMPO_VALOR_PADRAO, criado.getValorPadrao());
        response.put(CAMPO_ATIVO, criado.getAtivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos os procedimentos (ativos e inativos) do profissional.
     * Usado na tela de configurações para gerenciamento completo.
     * GET /procedimentos-padrao/profissional/{profissionalId}/todos
     */
    @GetMapping("/profissional/{profissionalId}/todos")
    public ResponseEntity<List<Map<String, Object>>> listarTodos(@PathVariable Long profissionalId) {

        List<ProcedimentoPadrao> lista = procedimentoService.listarTodosPorProfissional(profissionalId);

        List<Map<String, Object>> response = lista.stream().map(p -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", p.getId());
            item.put(CAMPO_NOME_PROCEDIMENTO, p.getNomeProcedimento());
            item.put(CAMPO_VALOR_PADRAO, p.getValorPadrao());
            item.put(CAMPO_ATIVO, p.getAtivo());
            return item;
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Alterna o status ativo/inativo de um procedimento.
     * PATCH /procedimentos-padrao/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAtivo(@PathVariable Long id) {

        ProcedimentoPadrao atualizado = procedimentoService.toggleAtivo(id);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", atualizado.getId());
        response.put(CAMPO_NOME_PROCEDIMENTO, atualizado.getNomeProcedimento());
        response.put(CAMPO_VALOR_PADRAO, atualizado.getValorPadrao());
        response.put(CAMPO_ATIVO, atualizado.getAtivo());

        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza um procedimento padrão.
     * PUT /procedimentos-padrao/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizar(
            @PathVariable Long id,
            @RequestBody ProcedimentoPadraoRequest request) {


        ProcedimentoPadrao atualizado = procedimentoService.atualizar(id, request);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", atualizado.getId());
        response.put(CAMPO_NOME_PROCEDIMENTO, atualizado.getNomeProcedimento());
        response.put(CAMPO_VALOR_PADRAO, atualizado.getValorPadrao());

        return ResponseEntity.ok(response);
    }

}
