package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
import br.com.saudeConecta.presentation.dto.mensageria.MensageriaResponse;
import br.com.saudeConecta.service.MensageriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento do sistema de mensageria.
 * Expõe endpoints para listagem, consulta e atualização de mensagens registradas.
 */
@RestController
@RequestMapping("/mensageria")
@RequiredArgsConstructor
@Slf4j
public class MensageriaController {

    private final MensageriaService mensageriaService;

    /**
     * Lista todas as mensagens da organização com paginação e filtros opcionais.
     *
     * @param status Filtro por status (PENDENTE, ENVIADO, FALHOU, RENOTIFICADO)
     * @param tipo   Filtro por tipo de mensagem
     * @param page   Número da página (padrão: 0)
     * @param size   Tamanho da página (padrão: 20)
     * @return Página de mensagens
     */
    @GetMapping
    public ResponseEntity<Page<MensageriaResponse>> listarMensagens(
            @RequestParam(required = false) StatusMensagem status,
            @RequestParam(required = false) TipoMensagem tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listando mensagens - status: {}, tipo: {}, page: {}", status, tipo, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        Page<MensageriaResponse> resultado = mensageriaService.listarMensagens(status, tipo, pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Busca uma mensagem específica por ID.
     *
     * @param id ID da mensagem
     * @return Dados da mensagem
     */
    @GetMapping("/{id}")
    public ResponseEntity<MensageriaResponse> buscarPorId(@PathVariable Long id) {
        log.info("Buscando mensagem por ID: {}", id);
        try {
            return ResponseEntity.ok(mensageriaService.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            log.warn("Mensagem nao encontrada: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lista mensagens com falha ainda não notificadas ao administrador.
     *
     * @return Lista de falhas pendentes de notificação
     */
    @GetMapping("/falhas-pendentes")
    public ResponseEntity<List<MensageriaResponse>> listarFalhasPendentes() {
        log.info("Listando falhas pendentes de notificacao");
        return ResponseEntity.ok(mensageriaService.listarFalhasPendentesNotificacao());
    }

    /**
     * Retorna a contagem de falhas pendentes de notificação (para badge de alerta).
     *
     * @return Mapa com a contagem de falhas pendentes
     */
    @GetMapping("/contagem-falhas")
    public ResponseEntity<Map<String, Long>> contarFalhasPendentes() {
        long total = mensageriaService.contarFalhasPendentes();
        return ResponseEntity.ok(Map.of("total", total));
    }

    /**
     * Marca uma mensagem como notificada ao administrador.
     *
     * @param id ID da mensagem
     * @return Resposta sem conteúdo
     */
    @PatchMapping("/{id}/notificar")
    public ResponseEntity<Void> marcarComoNotificado(@PathVariable Long id) {
        log.info("Marcando mensagem {} como notificada", id);
        try {
            mensageriaService.marcarComoNotificado(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao marcar mensagem como notificada: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reenvia uma mensagem para o destinatário original.
     *
     * @param id ID da mensagem
     * @return Resposta sem conteúdo em caso de sucesso
     */
    @PostMapping("/{id}/reenviar")
    public ResponseEntity<?> reenviarMensagem(@PathVariable Long id) {
        log.info("Reenviando mensagem ID: {}", id);
        try {
            mensageriaService.reenviarMensagem(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Erro ao reenviar mensagem: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Mensagem não encontrada para reenvio: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
