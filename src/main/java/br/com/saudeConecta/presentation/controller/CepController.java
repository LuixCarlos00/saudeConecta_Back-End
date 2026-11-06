package br.com.saudeConecta.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.presentation.erro.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/cep")
@RequiredArgsConstructor
@Slf4j
public class CepController {

    private final RestTemplate restTemplate;
    private static final String VIACEP_URL = "https://viacep.com.br/ws";

    /**
     * Busca endereço pelo CEP usando a API ViaCEP como proxy
     * @param cep CEP para busca (apenas números ou formatado)
     * @return ResponseEntity com dados do endereço
     */
    @GetMapping("/{cep}")
    public ResponseEntity<?> buscarEnderecoPorCep(@PathVariable String cep) {
        try {
            log.debug("cep.buscar inicio cep={}", MascaraLog.cep(cep));
            
            // Remove caracteres não numéricos do CEP
            String cepLimpo = cep.replaceAll("[^0-9]", "");
            
            if (cepLimpo.length() != 8) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "CEP deve conter 8 dígitos"));
            }

            String url = VIACEP_URL + "/" + cepLimpo + "/json";
            log.debug("cep.buscar viacep cep={}", MascaraLog.cep(cepLimpo));
            
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            
            if (response.getBody() instanceof Map) {
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                
                // Verifica se o CEP foi encontrado
                if (responseBody.containsKey("erro") && Boolean.TRUE.equals(responseBody.get("erro"))) {
                    log.warn("cep.buscar negado motivo=NAO_ENCONTRADO cep={}", MascaraLog.cep(cep));
                    return ResponseEntity.notFound().build();
                }
                
                // Verifica se os campos essenciais estão vazios
                if (!responseBody.containsKey("logradouro") && !responseBody.containsKey("localidade")) {
                    log.warn("cep.buscar negado motivo=CEP_INCOMPLETO cep={}", MascaraLog.cep(cep));
                    return ResponseEntity.notFound().build();
                }
            }
            
            log.debug("cep.buscar ok cep={}", MascaraLog.cep(cep));
            return ResponseEntity.ok(response.getBody());
            
        } catch (RestClientException e) {
            // ViaCEP fora do ar/lento é falha do serviço externo, não nossa: 502 com mensagem para o usuário
            log.warn("cep.buscar falha motivo=VIACEP_INDISPONIVEL cep={}", MascaraLog.cep(cep), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(HttpStatus.BAD_GATEWAY.value(), "CEP_INDISPONIVEL",
                        "Servico de CEP indisponivel no momento. Tente novamente.", "/cep/" + cep));
        }
    }
}
