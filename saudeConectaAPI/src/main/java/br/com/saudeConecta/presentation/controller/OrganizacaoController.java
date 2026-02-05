package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.OrganizacaoService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.presentation.dto.organizacao.AtualizarOrganizacaoRequest;
import br.com.saudeConecta.presentation.dto.organizacao.CriarOrganizacaoRequest;
import br.com.saudeConecta.presentation.dto.organizacao.OrganizacaoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/organizacoes")
@RequiredArgsConstructor
@Slf4j
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    @GetMapping("/minha")
    public ResponseEntity<OrganizacaoResponse> buscarMinhaOrganizacao() {
        log.debug("Buscando organização do usuário atual");
        return organizacaoService.buscarAtual()
            .map(OrganizacaoResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrganizacaoResponse>> listarTodas() {
        log.debug("Listando todas as organizações");
        List<OrganizacaoResponse> response = organizacaoService.listarTodas()
            .stream()
            .map(OrganizacaoResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ativas")
    public ResponseEntity<List<OrganizacaoResponse>> listarAtivas() {
        log.debug("Listando organizações ativas");
        List<OrganizacaoResponse> response = organizacaoService.listarAtivas()
            .stream()
            .map(OrganizacaoResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizacaoResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando organização por ID: {}", id);
        return organizacaoService.buscarPorId(id)
            .map(OrganizacaoResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrganizacaoResponse> criar(@Valid @RequestBody CriarOrganizacaoRequest request) {
        log.info("Criando nova organização: {}", request.nome());
        Organizacao org = organizacaoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrganizacaoResponse.fromEntity(org));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizacaoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarOrganizacaoRequest request) {
        log.info("Atualizando organização ID: {}", id);
        Organizacao org = organizacaoService.atualizar(id, request);
        return ResponseEntity.ok(OrganizacaoResponse.fromEntity(org));
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        log.info("Ativando organização ID: {}", id);
        organizacaoService.ativar(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        log.info("Inativando organização ID: {}", id);
        organizacaoService.inativar(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/estatisticas/total")
    public ResponseEntity<Long> contarAtivas() {
        return ResponseEntity.ok(organizacaoService.contarAtivas());
    }
}
