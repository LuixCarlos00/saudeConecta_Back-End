package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.OrganizacaoService;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.presentation.dto.organizacao.AtualizarOrganizacaoRequest;
import br.com.saudeConecta.presentation.dto.organizacao.CriarOrganizacaoRequest;
import br.com.saudeConecta.presentation.dto.organizacao.OrganizacaoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/organizacoes")
@RequiredArgsConstructor
@Slf4j
// Somente GET /minha é do tenant do usuário; as demais rotas são operações de plataforma
// (listar/criar/alterar/ativar/inativar qualquer clínica) e ficam restritas ao SUPER_ADMIN.
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    @GetMapping("/minha")
    public ResponseEntity<OrganizacaoResponse> buscarMinhaOrganizacao() {
        log.debug("Buscando organizacao do usuario atual");
        return organizacaoService.buscarAtual()
            .map(OrganizacaoResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<OrganizacaoResponse>> listarTodas() {
        log.debug("Listando todas as organizacoes");
        List<OrganizacaoResponse> response = organizacaoService.listarTodas()
            .stream()
            .map(OrganizacaoResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ativas")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<OrganizacaoResponse>> listarAtivas() {
        log.debug("Listando organizacoes ativas");
        List<OrganizacaoResponse> response = organizacaoService.listarAtivas()
            .stream()
            .map(OrganizacaoResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizacaoResponse> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando organizacao por ID: {}", id);
        return organizacaoService.buscarPorId(id)
            .map(OrganizacaoResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizacaoResponse> criar(@Valid @RequestBody CriarOrganizacaoRequest request) {
        log.info("Criando nova organizacao: {}", request.nome());
        Organizacao org = organizacaoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrganizacaoResponse.fromEntity(org));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrganizacaoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarOrganizacaoRequest request) {
        log.info("Atualizando organizacao ID: {}", id);
        Organizacao org = organizacaoService.atualizar(id, request);
        return ResponseEntity.ok(OrganizacaoResponse.fromEntity(org));
    }

    @PutMapping("/{id}/ativar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        log.info("Ativando organizacao ID: {}", id);
        organizacaoService.ativar(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/inativar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        log.info("Inativando organizacao ID: {}", id);
        organizacaoService.inativar(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/estatisticas/total")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> contarAtivas() {
        return ResponseEntity.ok(organizacaoService.contarAtivas());
    }
}
