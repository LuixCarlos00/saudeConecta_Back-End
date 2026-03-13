package br.com.saudeConecta.presentation.controller;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.presentation.dto.prontuario.CadastrarProntuarioDentistaRequest;
import br.com.saudeConecta.service.ProntuarioDentistaService;
import br.com.saudeConecta.service.TermoAutorizacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/prontuario-dentista")
@RequiredArgsConstructor
public class ProntuarioDentistaController {

    private final ProntuarioDentistaService prontuarioDentistaService;
    private final TermoAutorizacaoService termoAutorizacaoService;

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
     * Atualiza um prontuário odontológico existente.
     * PUT /prontuario-dentista/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> atualizarProntuario(
            @PathVariable Long id,
            @RequestBody CadastrarProntuarioDentistaRequest request) {

        log.info("PUT /prontuario-dentista/{} — atualizando prontuário", id);
        prontuarioDentistaService.atualizarProntuario(id, request);
        return ResponseEntity.ok().build();
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

    /**
     * Lista prontuários de um paciente (histórico).
     * GET /prontuario-dentista/paciente/{pacienteId}
     */
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<ProntuarioDentista>> listarPorPaciente(
            @PathVariable Long pacienteId) {
        log.info("GET /prontuario-dentista/paciente/{}", pacienteId);
        return ResponseEntity.ok(
                prontuarioDentistaService.listarPorPaciente(pacienteId));
    }

    /**
     * Gera link do questionário de saúde para uma consulta.
     * POST /prontuario-dentista/gerar-link-questionario/{consultaId}
     */
    @PostMapping("/gerar-link-questionario/{consultaId}")
    public ResponseEntity<Map<String, String>> gerarLinkQuestionario(
            @PathVariable Long consultaId) {

        log.info("POST /prontuario-dentista/gerar-link-questionario/{}", consultaId);

        String token = termoAutorizacaoService.gerarLinkQuestionario(consultaId);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca o questionário respondido de uma consulta (para exibir na aba do prontuário).
     * GET /prontuario-dentista/questionario-saude/{consultaId}
     */
    @GetMapping("/questionario-saude/{consultaId}")
    public ResponseEntity<Map<String, Object>> buscarQuestionarioSaude(
            @PathVariable Long consultaId) {

        log.info("GET /prontuario-dentista/questionario-saude/{}", consultaId);

        TermoAutorizacao termo = termoAutorizacaoService.buscarPorConsultaId(consultaId);

        if (termo == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("respondido", false);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("respondido", termo.isAssinado());
        response.put("status", termo.getStatus());
        response.put("respostasQuestionario", termo.getRespostasQuestionario());
        response.put("assinaturaBase64", termo.getAssinaturaBase64());
        response.put("dataAssinatura", termo.getDataAssinatura());
        response.put("ipOrigem", termo.getIpOrigem());

        return ResponseEntity.ok(response);
    }
}
