package br.com.saudeConecta.presentation.controller.mensageria;

import br.com.saudeConecta.application.mensageria.MensageriaService;
import br.com.saudeConecta.application.mensageria.dto.MensageriaResponse;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        Page<MensageriaResponse> resultado = mensageriaService.listarMensagens(status, tipo, pageable);
        return ResponseEntity.ok(resultado);
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
        try {
            mensageriaService.marcarComoNotificado(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("mensageria.notificar negado motivo=NAO_ENCONTRADA_OU_ACESSO_NEGADO mensageriaId={} detalhe={}", id, e.getMessage());
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
    public ResponseEntity<Void> reenviarMensagem(@PathVariable Long id) {
        // Regra de negócio (IllegalStateException) sobe para o handler global e vira 409
        try {
            mensageriaService.reenviarMensagem(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("mensageria.reenviar negado motivo=NAO_ENCONTRADA mensageriaId={}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
