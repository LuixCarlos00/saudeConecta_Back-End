package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.SecretariaService;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaCompletoRequest;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/secretaria")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description("Endpoints para gerenciamento de secretárias")
public class SecretariaController {

    private final SecretariaService secretariaService;

    @GetMapping("/buscarId/{id}")
    @Description("Busca secretária por ID")
    public ResponseEntity<Secretaria> buscarPorId(@PathVariable Long id) {
        log.debug("Buscando secretária por ID: {}", id);
        return secretariaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/buscarIdUsuario/{idUsuario}")
    @Description("Busca secretária por ID do usuário")
    public ResponseEntity<Secretaria> buscarPorIdUsuario(@PathVariable Long idUsuario) {
        log.debug("Buscando secretária por ID de usuário: {}", idUsuario);
        return secretariaService.buscarPorIdUsuario(idUsuario)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/listarTodos")
    @Description("Lista todas as secretárias")
    public ResponseEntity<List<Secretaria>> listarTodos() {
        log.debug("Listando todas as secretárias");
        return ResponseEntity.ok(secretariaService.buscarTodos());
    }

    @PostMapping("/cadastrar")
    @Description("Cadastra nova secretária com criação automática de usuário (CPF como login, senha gerada e enviada por email)")
    public ResponseEntity<?> cadastrarSecretaria(
            @RequestBody @Valid CadastrarSecretariaCompletoRequest dados,
            UriComponentsBuilder uriBuilder) {
        
        log.info("Recebida requisição de cadastro para secretária: {}", dados.secreNome());
        
        try {
            long startTime = System.currentTimeMillis();
            Secretaria secretariaSalva = secretariaService.cadastrarCompleto(dados);
            long endTime = System.currentTimeMillis();
            
            log.info("Secretária cadastrada com sucesso em {}ms. ID: {}", endTime - startTime, secretariaSalva.getSecreCodigo());

            URI uri = uriBuilder.path("/secretaria/buscarId/{id}")
                    .buildAndExpand(secretariaSalva.getSecreCodigo())
                    .toUri();

            return ResponseEntity.created(uri).body(secretariaSalva);
        } catch (IllegalStateException e) {
            log.warn("CPF já cadastrado: {}", dados.secreCpf());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cadastrar secretária: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao cadastrar secretária");
        }
    }

    @DeleteMapping("/{id}")
    @Description("Exclui secretária por ID")
    public ResponseEntity<Void> deletarSecretaria(@PathVariable Long id) {
        log.debug("Deletando secretária por ID: {}", id);
        try {
            secretariaService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erro ao deletar secretária: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
