package br.com.saudeConecta.presentation.controller.prontuario;
import br.com.saudeConecta.application.prontuario.ProntuarioDentistaService;
import br.com.saudeConecta.application.prontuario.TermoAutorizacaoService;
import br.com.saudeConecta.application.prontuario.dto.CadastrarProntuarioDentistaRequest;
import br.com.saudeConecta.domain.prontuario.ProntuarioDentista;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

        prontuarioDentistaService.atualizarProntuario(id, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Retorna o prontuário mais recente de uma consulta.
     * GET /prontuario-dentista/consulta/{consultaId}/recente
     */
    @GetMapping("/consulta/{consultaId}/recente")
    public ResponseEntity<ProntuarioDentista> buscarMaisRecente(
            @PathVariable Long consultaId) {
        return ResponseEntity.ok(
                prontuarioDentistaService.buscarMaisRecentePorConsulta(consultaId));
    }

    /**
     * Gera link do questionário de saúde para uma consulta.
     * POST /prontuario-dentista/gerar-link-questionario/{consultaId}
     */
    @PostMapping("/gerar-link-questionario/{consultaId}")
    public ResponseEntity<Map<String, String>> gerarLinkQuestionario(
            @PathVariable Long consultaId) {

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
