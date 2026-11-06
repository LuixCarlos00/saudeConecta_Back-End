package br.com.saudeConecta.infrastructure.viacep;

import br.com.saudeConecta.application.endereco.port.BuscaCep;
import br.com.saudeConecta.application.endereco.port.CepIndisponivelException;
import br.com.saudeConecta.application.endereco.port.CepNaoEncontradoException;
import br.com.saudeConecta.config.logging.MascaraLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adaptador do ViaCEP (antes dentro do {@code CepController}, fase B6). Usa o {@link RestTemplate}
 * da aplicação, com os timeouts de {@code RestTemplateConfig} ({@code app.http.*}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViaCepClient implements BuscaCep {

    private static final String VIACEP_URL = "https://viacep.com.br/ws";

    private final RestTemplate restTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object buscar(String cep) {
        String url = VIACEP_URL + "/" + cep + "/json";
        log.debug("cep.buscar viacep cep={}", MascaraLog.cep(cep));

        ResponseEntity<Object> response;
        try {
            response = restTemplate.getForEntity(url, Object.class);
        } catch (RestClientException e) {
            // ViaCEP fora do ar/lento é falha do serviço externo, não nossa
            log.warn("cep.buscar falha motivo=VIACEP_INDISPONIVEL cep={}", MascaraLog.cep(cep), e);
            throw new CepIndisponivelException("ViaCEP indisponivel", e);
        }

        if (response.getBody() instanceof Map<?, ?> responseBody) {
            // Verifica se o CEP foi encontrado
            if (responseBody.containsKey("erro") && Boolean.TRUE.equals(responseBody.get("erro"))) {
                log.warn("cep.buscar negado motivo=NAO_ENCONTRADO cep={}", MascaraLog.cep(cep));
                throw new CepNaoEncontradoException("CEP nao encontrado");
            }

            // Verifica se os campos essenciais estão vazios
            if (!responseBody.containsKey("logradouro") && !responseBody.containsKey("localidade")) {
                log.warn("cep.buscar negado motivo=CEP_INCOMPLETO cep={}", MascaraLog.cep(cep));
                throw new CepNaoEncontradoException("CEP sem logradouro e sem localidade");
            }
        }
        return response.getBody();
    }
}
