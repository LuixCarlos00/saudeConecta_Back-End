package br.com.saudeConecta.presentation.controller;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioDentistaRequest;
import br.com.saudeConecta.service.ProntuarioDentistaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/prontuario-dentista")
@RequiredArgsConstructor
public class ProntuarioDentistaController {

    private final ProntuarioDentistaService prontuarioDentistaService;

    /**
     * Finaliza a consulta odontológica.
     * POST /prontuario-dentista
     */
    @PostMapping("/cadastrarProntuarioByOrg")
    public ResponseEntity<Void> cadastrarProntuarioByOrg(
            @RequestBody CadastrarProntuarioDentistaRequest request) {

        log.info("POST /prontuario-dentista/cadastrar — consulta={} profissional={}",
                request.getConsulta(), request.getCodigoMedico());

        prontuarioDentistaService.cadastrarProntuarioByOrg(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Busca prontuário por ID (inclui odontograma completo para recarregar o SVG).
     * GET /prontuario-dentista/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProntuarioDentista> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(prontuarioDentistaService.buscarPorId(id));
    }

    /**
     * Lista prontuários de uma consulta.
     * GET /prontuario-dentista/consulta/{consultaId}
     */
    @GetMapping("/consulta/{consultaId}")
    public ResponseEntity<List<ProntuarioDentista>> listarPorConsulta(
            @PathVariable Long consultaId) {
        return ResponseEntity.ok(prontuarioDentistaService.listarPorConsulta(consultaId));
    }

    /**
     * Retorna o prontuário mais recente de uma consulta.
     * GET /prontuario-dentista/consulta/{consultaId}/recente
     */
    @GetMapping("/consulta/{consultaId}/recente")
    public ResponseEntity<ProntuarioDentista> buscarMaisRecente(
            @PathVariable Long consultaId) {
        log.info("  /prontuario-dentista/consulta/{}/recente", consultaId);
        return ResponseEntity.ok(
                prontuarioDentistaService.buscarMaisRecentePorConsulta(consultaId));
    }

    /**
     * Lista prontuários de um profissional.
     * GET /prontuario-dentista/profissional/{profissionalId}
     */
    @GetMapping("/profissional/{profissionalId}")
    public ResponseEntity<List<ProntuarioDentista>> listarPorProfissional(
            @PathVariable Long profissionalId) {
        return ResponseEntity.ok(
                prontuarioDentistaService.listarPorProfissional(profissionalId));
    }
}
