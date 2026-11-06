package br.com.saudeConecta.presentation.controller.atendimento;

import br.com.saudeConecta.application.atendimento.AtendimentoRascunhoService;
import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResponse;
import br.com.saudeConecta.application.atendimento.dto.RascunhoAtendimentoResumoResponse;
import br.com.saudeConecta.application.atendimento.dto.SalvarRascunhoAtendimentoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rascunho do atendimento: o profissional sai da tela de atendimento e volta (F5, outro computador)
 * sem perder o que digitou. Só o profissional dono da consulta, na clínica do token, acessa.
 */
@RestController
@RequestMapping("/atendimentos")
@RequiredArgsConstructor
public class AtendimentoRascunhoController {

    private final AtendimentoRascunhoService rascunhoService;

    /**
     * {@code GET /atendimentos/{consultaId}/rascunho}.
     *
     * @param consultaId ID da consulta
     * @return 200 com o rascunho, 204 se não houver, 404 se a consulta não for do profissional logado
     */
    @GetMapping("/{consultaId}/rascunho")
    public ResponseEntity<RascunhoAtendimentoResponse> buscar(@PathVariable Long consultaId) {
        return rascunhoService.buscar(consultaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * {@code PUT /atendimentos/{consultaId}/rascunho}: cria ou atualiza.
     *
     * @param consultaId ID da consulta
     * @param request    tipo, dados, cronômetro e versão lida por último ({@code null} na primeira gravação)
     * @return 200 com a versão nova; 409 {@code RASCUNHO_DESATUALIZADO}/{@code CONSULTA_ENCERRADA}; 422 inválido
     */
    @PutMapping("/{consultaId}/rascunho")
    public ResponseEntity<RascunhoAtendimentoResponse> salvar(@PathVariable Long consultaId,
                                                              @RequestBody SalvarRascunhoAtendimentoRequest request) {
        return ResponseEntity.ok(rascunhoService.salvar(consultaId, request));
    }

    /**
     * {@code DELETE /atendimentos/{consultaId}/rascunho}: "Descartar rascunho". Idempotente.
     *
     * @param consultaId ID da consulta
     * @return 204, existindo rascunho ou não
     */
    @DeleteMapping("/{consultaId}/rascunho")
    public ResponseEntity<Void> descartar(@PathVariable Long consultaId) {
        rascunhoService.descartar(consultaId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /atendimentos/rascunhos/meus}: rascunhos do profissional logado (agenda, "Retomar atendimento").
     *
     * @return 200 com a lista (vazia se o usuário não for profissional)
     */
    @GetMapping("/rascunhos/meus")
    public ResponseEntity<List<RascunhoAtendimentoResumoResponse>> listarMeus() {
        return ResponseEntity.ok(rascunhoService.listarMeus());
    }
}
