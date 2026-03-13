package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
import br.com.saudeConecta.presentation.dto.prontuario.AssinaturaPlanjamentoRequest;
import br.com.saudeConecta.presentation.dto.prontuario.QuestionarioSaudeRequest;
import br.com.saudeConecta.service.PlanejamentoTerapeuticoService;
import br.com.saudeConecta.service.TermoAutorizacaoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller PÚBLICO (sem autenticação) para:
 * - Questionário de Saúde do paciente
 * - Assinatura do Planejamento Terapêutico
 */
@Slf4j
@RestController
@RequestMapping("/publico")
@RequiredArgsConstructor
public class QuestionarioPublicoController {

    private final TermoAutorizacaoService termoService;
    private final PlanejamentoTerapeuticoService planejamentoService;

    // =========================================================================
    // QUESTIONÁRIO DE SAÚDE
    // =========================================================================

    /**
     * Busca dados do questionário pelo token (exibe o formulário ao paciente).
     * GET /publico/questionario/{token}
     */
    @GetMapping("/questionario/{token}")
    public ResponseEntity<Map<String, Object>> buscarQuestionario(@PathVariable String token) {
        log.info("GET /publico/questionario/{}", token);

        TermoAutorizacao termo = termoService.buscarPorToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("token", termo.getToken());
        response.put("pacienteNome", termo.getPaciente().getPaciNome());
        response.put("status", termo.getStatus());
        response.put("profissionalNome", termo.getConsulta().getProfissional().getNome());
        response.put("clinicaNome",termo.getOrganizacao().getNome());
        response.put("dataExpiracao", termo.getDataExpiracao());

        return ResponseEntity.ok(response);
    }

    /**
     * Recebe as respostas do questionário + assinatura digital.
     * POST /publico/questionario/responder
     */
    @PostMapping("/questionario/responder")
    public ResponseEntity<Map<String, String>> responderQuestionario(
            @RequestBody QuestionarioSaudeRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigem = obterIpOrigem(httpRequest);
        log.info("POST /publico/questionario/responder — token={}, ip={}", request.getToken(), ipOrigem);

        termoService.responderQuestionario(request, ipOrigem);

        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Questionário respondido e assinado com sucesso!");

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ASSINATURA DO PLANEJAMENTO TERAPÊUTICO
    // =========================================================================

    /**
     * Busca planejamentos pelo token de assinatura (exibe resumo ao paciente).
     * GET /publico/planejamento/{token}
     */
    @GetMapping("/planejamento/{token}")
    public ResponseEntity<Map<String, Object>> buscarPlanejamento(@PathVariable String token) {
        log.info("GET /publico/planejamento/{}", token);

        List<PlanejamentoTerapeutico> planejamentos = planejamentoService.buscarPorToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("totalItens", planejamentos.size());

        // Dados do paciente, profissional e clínica (extraídos do primeiro item)
        PlanejamentoTerapeutico primeiro = planejamentos.get(0);
        response.put("pacienteNome", primeiro.getPaciente() != null ? primeiro.getPaciente().getPaciNome() : "");
        response.put("profissionalNome", primeiro.getProfissional() != null ? primeiro.getProfissional().getNome() : "");
        response.put("clinicaNome", primeiro.getOrganizacao() != null ? primeiro.getOrganizacao().getNome() : "");
        response.put("assinado", planejamentos.stream().allMatch(PlanejamentoTerapeutico::isAssinado));

        List<Map<String, Object>> itens = planejamentos.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("dataProcedimento", p.getDataProcedimento());
            item.put("procedimentoRealizado", p.getProcedimentoRealizado());
            item.put("valor", p.getValor());
            item.put("statusAssinatura", p.getStatusAssinatura());
            return item;
        }).toList();

        response.put("itens", itens);
        return ResponseEntity.ok(response);
    }

    /**
     * Assina todos os itens do planejamento vinculados ao token.
     * POST /publico/planejamento/assinar
     */
    @PostMapping("/planejamento/assinar")
    public ResponseEntity<Map<String, String>> assinarPlanejamento(
            @RequestBody AssinaturaPlanjamentoRequest request) {

        log.info("POST /publico/planejamento/assinar — token={}", request.getToken());

        planejamentoService.assinarPorToken(request.getToken(), request.getAssinaturaBase64());

        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Planejamento terapêutico assinado com sucesso!");

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // UTILITÁRIO
    // =========================================================================

    private String obterIpOrigem(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
