package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.suporte.PrioridadeChamado;
import br.com.saudeConecta.domain.suporte.StatusChamado;
import br.com.saudeConecta.presentation.dto.suporte.AtualizarStatusChamadoRequest;
import br.com.saudeConecta.presentation.dto.suporte.ChamadoSuporteRequest;
import br.com.saudeConecta.presentation.dto.suporte.ChamadoSuporteResponse;
import br.com.saudeConecta.service.ChamadoSuporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para abertura e gestao de chamados de suporte.
 */
@RestController
@RequestMapping("/chamados-suporte")
@RequiredArgsConstructor
@Slf4j
public class ChamadoSuporteController {

    private final ChamadoSuporteService chamadoSuporteService;

    /**
     * Cadastra um novo chamado de suporte para a organizacao do usuario autenticado.
     *
     * @param request dados do chamado
     * @return chamado criado
     */
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody @Valid ChamadoSuporteRequest request) {
        log.info("Cadastrando chamado de suporte: {}", request.titulo());
        try {
            ChamadoSuporteResponse response = chamadoSuporteService.cadastrar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Erro ao cadastrar chamado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lista os chamados da organizacao atual com paginacao e filtros opcionais.
     *
     * @param status     filtro por status (opcional)
     * @param prioridade filtro por prioridade (opcional)
     * @param page       numero da pagina
     * @param size       tamanho da pagina
     * @return pagina de chamados
     */
    @GetMapping
    public ResponseEntity<Page<ChamadoSuporteResponse>> listarPorTenant(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) PrioridadeChamado prioridade,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listando chamados de suporte - status: {}, prioridade: {}, page: {}", status, prioridade, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        return ResponseEntity.ok(chamadoSuporteService.listarPorTenant(status, prioridade, pageable));
    }

    /**
     * Busca um chamado especifico com seus anexos.
     *
     * @param id ID do chamado
     * @return chamado encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChamadoSuporteResponse> buscarPorId(@PathVariable String id) {
        log.info("Buscando chamado de suporte ID: {}", id);
        try {
            return ResponseEntity.ok(chamadoSuporteService.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            log.warn("Chamado nao encontrado: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Atualiza o status de um chamado. Disponivel apenas para o ROOT (suporte).
     * O autor do chamado e notificado por email.
     *
     * @param id      ID do chamado
     * @param request novo status e observacao opcional
     * @return chamado atualizado
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable String id,
                                             @RequestBody @Valid AtualizarStatusChamadoRequest request) {
        log.info("Atualizando status do chamado {} para {}", id, request.status());
        try {
            return ResponseEntity.ok(chamadoSuporteService.atualizarStatus(id, request));
        } catch (IllegalStateException e) {
            log.warn("Acesso negado na atualizacao de status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Chamado nao encontrado para atualizacao: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Exclui um chamado da organizacao atual.
     *
     * @param id ID do chamado
     * @return resposta sem conteudo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        log.info("Excluindo chamado de suporte ID: {}", id);
        try {
            chamadoSuporteService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao excluir chamado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
