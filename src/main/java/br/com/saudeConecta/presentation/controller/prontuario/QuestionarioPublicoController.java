package br.com.saudeConecta.presentation.controller.prontuario;

import br.com.saudeConecta.application.prontuario.PlanejamentoTerapeuticoService;
import br.com.saudeConecta.application.prontuario.TermoAutorizacaoService;
import br.com.saudeConecta.application.prontuario.dto.AssinaturaPlanjamentoRequest;
import br.com.saudeConecta.application.prontuario.dto.QuestionarioSaudeRequest;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.domain.prontuario.PlanejamentoTerapeutico;
import br.com.saudeConecta.domain.prontuario.TermoAutorizacao;
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
        log.info("questionario.publico consultar token={}", MascaraLog.token(token));

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
        log.info("questionario.responder recebido token={} ip={}",
                MascaraLog.token(request.getToken()), ipOrigem);

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
        log.info("planejamento.publico consultar token={}", MascaraLog.token(token));

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
            @RequestBody AssinaturaPlanjamentoRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigem = obterIpOrigem(httpRequest);
        log.info("planejamento.assinar recebido token={} ip={}",
                MascaraLog.token(request.getToken()), ipOrigem);

        planejamentoService.assinarPorToken(request.getToken(), request.getAssinaturaBase64(), ipOrigem);

        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Planejamento terapêutico assinado com sucesso!");

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // UTILITÁRIO
    // =========================================================================

    /**
     * IP de origem gravado como evidência da assinatura.
     * Não lê X-Forwarded-For diretamente (o cliente pode forjar o cabeçalho): o IP real
     * atrás do proxy é resolvido pelo servidor (server.forward-headers-strategy), que só
     * confia nos proxies internos configurados.
     *
     * @param request requisição HTTP
     * @return IP de origem resolvido pelo servidor
     */
    private String obterIpOrigem(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
