package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.SecretariaService;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaRequest;
import br.com.saudeConecta.presentation.dto.secretaria.SecretariaResponse;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/secretaria")
@RequiredArgsConstructor
@Slf4j
public class SecretariaController {

    private final SecretariaService secretariaService;

    @GetMapping
    @Description("Lista todas as secretárias da organização")
    public ResponseEntity<List<SecretariaResponse>> listarTodas() {
        List<Secretaria> secretarias = secretariaService.buscarTodas();
        List<SecretariaResponse> response = secretarias.stream()
            .map(SecretariaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ativas")
    @Description("Lista secretárias ativas da organização")
    public ResponseEntity<List<SecretariaResponse>> listarAtivas() {
        List<Secretaria> secretarias = secretariaService.buscarAtivas();
        List<SecretariaResponse> response = secretarias.stream()
            .map(SecretariaResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Description("Busca secretária por ID")
    public ResponseEntity<SecretariaResponse> buscarPorId(@PathVariable Long id) {
        return secretariaService.buscarPorId(id)
            .map(SecretariaResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cadastrarSecretariaByOrg")
    @Description("Cadastra nova secretária. Utilizado em: CadastroSecretariaComponent")
    public ResponseEntity<?> cadastrarSecretariaByOrg(@RequestBody @Valid CadastrarSecretariaRequest request) {
        log.info("Cadastrando secretária: {}", request.secreNome());
        try {
            Secretaria secretaria = secretariaService.cadastrar(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(SecretariaResponse.fromEntity(secretaria));
        } catch (IllegalStateException e) {
            log.warn("Erro ao cadastrar secretária: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Description("Inativa secretária por ID")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        secretariaService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    @Description("Conta secretárias ativas da organização")
    public ResponseEntity<Long> contarAtivas() {
        return ResponseEntity.ok(secretariaService.contarAtivas());
    }
}
