package br.com.saudeConecta.presentation.controller.secretaria;

import br.com.saudeConecta.application.secretaria.SecretariaService;
import br.com.saudeConecta.application.secretaria.dto.AtualizarSecretariaRequest;
import br.com.saudeConecta.application.secretaria.dto.CadastrarSecretariaRequest;
import br.com.saudeConecta.application.secretaria.dto.SecretariaResponse;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/secretaria")
@RequiredArgsConstructor
@Slf4j
public class SecretariaController {

    private final SecretariaService secretariaService;


    @PostMapping("/cadastrarSecretariaByOrg")
    @PreAuthorize("hasRole('ADMIN')")
    @Description("Cadastra nova secretária. Utilizado em: CadastroSecretariaComponent")
    public ResponseEntity<SecretariaResponse> cadastrarSecretariaByOrg(@RequestBody @Valid CadastrarSecretariaRequest request) {
        Secretaria secretaria = secretariaService.cadastrarSecretariaByOrg(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SecretariaResponse.fromEntity(secretaria));
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
    @Description("Atualiza secretária por ID. Gestor ou a própria secretária (regra no service)")
    public ResponseEntity<SecretariaResponse> atualizarSecretariaIdByOrg(@PathVariable Long id, @RequestBody @Valid AtualizarSecretariaRequest secretaria) {
        try {
            Secretaria atualizada = secretariaService.atualizarSecretariaIdByOrg(id, secretaria);
            return ResponseEntity.ok(SecretariaResponse.fromEntity(atualizada));
        } catch (IllegalArgumentException e) {
            log.warn("secretaria.atualizar negado motivo=NAO_ENCONTRADA secretariaId={}", id);
            return ResponseEntity.notFound().build();
        }
    }


    @DeleteMapping("/deletarSecretariaIdByOrg/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Description("Deleta secretária por ID")
    public ResponseEntity<Void> deletarSecretariaIdByOrg(@PathVariable Long id) {
        try {
            secretariaService.deletarSecretariaIdByOrg(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("secretaria.deletar negado motivo=NAO_ENCONTRADA secretariaId={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    //deletarSecretariaIdByOrg
}
