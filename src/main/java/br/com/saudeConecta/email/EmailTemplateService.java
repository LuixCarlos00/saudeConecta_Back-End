package br.com.saudeConecta.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * Serviço responsável exclusivamente pela renderização de templates Thymeleaf.
 * Recebe o nome do template e as variáveis, retorna o HTML como String.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Renderiza um template Thymeleaf com as variáveis fornecidas.
     *
     * @param nomeTemplate nome do arquivo de template (sem extensão .html)
     * @param variaveis    mapa de variáveis para o template
     * @return HTML renderizado como String
     */
    public String renderizar(String nomeTemplate, Map<String, Object> variaveis) {
        log.debug("Renderizando template: {}", nomeTemplate);
        Context contexto = new Context(Locale.getDefault(), variaveis);
        return templateEngine.process(nomeTemplate, contexto);
    }
}
