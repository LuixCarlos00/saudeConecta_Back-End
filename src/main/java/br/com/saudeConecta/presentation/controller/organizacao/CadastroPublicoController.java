package br.com.saudeConecta.presentation.controller.organizacao;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.organizacao.AdminOrganizacaoService;
import br.com.saudeConecta.application.organizacao.dto.CadastroSaasRequest;
import br.com.saudeConecta.config.web.RateLimiter;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auto-cadastro SaaS a partir da landing page.
 * Cria a organização com 30 dias de trial, sem exigir cartão.
 */
@RestController
@RequestMapping("/publico/cadastro")
@RequiredArgsConstructor
@Slf4j
public class CadastroPublicoController {

    /** Cadastros permitidos por IP por hora. */
    private static final int LIMITE_POR_IP = 5;
    private static final String CHAVE_RATE_LIMIT = "cadastro-saas:";

    private final AdminOrganizacaoService adminOrganizacaoService;
    private final RateLimiter rateLimiter;

    /**
     * Cria organização, administrador e assinatura em trial.
     *
     * @param request  dados do cadastro
     * @param servlet  requisição HTTP, usada para identificar o IP de origem
     * @return dados mínimos do tenant criado
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> cadastrar(
            @RequestBody @Valid CadastroSaasRequest request,
            HttpServletRequest servlet) {

        String ip = resolverIp(servlet);
        if (!rateLimiter.permitir(CHAVE_RATE_LIMIT + ip, LIMITE_POR_IP)) {
            log.warn("cadastro.publico negado motivo=RATE_LIMIT ip={}", ip);
            throw new BusinessException(
                    "Muitas tentativas de cadastro. Tente novamente mais tarde.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        AdminOrganizacao admin = adminOrganizacaoService.cadastrarTenantPublico(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "organizacaoId", admin.getOrganizacao().getId(),
                "adminId", admin.getId(),
                "mensagem", "Cadastro realizado. Enviamos as credenciais de acesso para o seu e-mail."
        ));
    }

    /**
     * Resolve o IP de origem. Com server.forward-headers-strategy=native o Tomcat já substitui o
     * remoteAddr pelo IP do cliente informado pelo proxy confiável; ler X-Forwarded-For na mão
     * permitiria forjar o cabeçalho para burlar o rate limit.
     */
    private String resolverIp(HttpServletRequest servlet) {
        return servlet.getRemoteAddr();
    }
}
