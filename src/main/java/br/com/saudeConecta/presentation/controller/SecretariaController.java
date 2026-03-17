package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.service.SecretariaService;
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

@RestController
@RequestMapping("/secretaria")
@RequiredArgsConstructor
@Slf4j
public class SecretariaController {

    private final SecretariaService secretariaService;


    @PostMapping("/cadastrarSecretariaByOrg")
    @Description("Cadastra nova secretária. Utilizado em: CadastroSecretariaComponent")
    public ResponseEntity<?> cadastrarSecretariaByOrg(@RequestBody @Valid CadastrarSecretariaRequest request) {
        log.info("Cadastrando secretaria: {}", request.nome());
        try {
            Secretaria secretaria = secretariaService.cadastrarSecretariaByOrg(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(SecretariaResponse.fromEntity(secretaria));
        } catch (IllegalStateException e) {
            log.warn("Erro ao cadastrar secretaria: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/buscarSecretariaIdByOrg/{idSecretaria}")
    @Description("Busca secretária por ID")
    public ResponseEntity<SecretariaResponse> buscarSecretariaIdByOrg(@PathVariable Long idSecretaria) {
        return secretariaService.buscarSecretariaIdByOrg(idSecretaria)
                .map(SecretariaResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/atualizarSecretariaIdByOrg/{id}")
    @Description("Atualiza secretária por ID")
    public ResponseEntity<SecretariaResponse> atualizarSecretariaIdByOrg(@PathVariable Long id, @RequestBody Secretaria secretaria) {
        try {
            Secretaria atualizada = secretariaService.atualizarSecretariaIdByOrg(id, secretaria);
            return ResponseEntity.ok(SecretariaResponse.fromEntity(atualizada));
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao atualizar secretaria: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @DeleteMapping("/deletarSecretariaIdByOrg/{id}")
    @Description("Deleta secretária por ID")
    public ResponseEntity<?> deletarSecretariaIdByOrg(@PathVariable Long id) {
        try {
            secretariaService.deletarSecretariaIdByOrg(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Erro ao deletar secretaria: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Secretaria nao encontrada: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    //deletarSecretariaIdByOrg
}
