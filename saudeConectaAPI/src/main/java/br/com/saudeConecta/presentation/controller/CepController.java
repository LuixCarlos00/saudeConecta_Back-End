package br.com.saudeConecta.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            log.debug("Buscando endereço para CEP: {}", cep);
            
            // Remove caracteres não numéricos do CEP
            String cepLimpo = cep.replaceAll("[^0-9]", "");
            
            if (cepLimpo.length() != 8) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "CEP deve conter 8 dígitos"));
            }

            String url = VIACEP_URL + "/" + cepLimpo + "/json";
            log.debug("Fazendo requisição para ViaCEP: {}", url);
            
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            
            if (response.getBody() instanceof Map) {
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                
                // Verifica se o CEP foi encontrado
                if (responseBody.containsKey("erro") && Boolean.TRUE.equals(responseBody.get("erro"))) {
                    log.warn("CEP não encontrado: {}", cep);
                    return ResponseEntity.notFound().build();
                }
                
                // Verifica se os campos essenciais estão vazios
                if (!responseBody.containsKey("logradouro") && !responseBody.containsKey("localidade")) {
                    log.warn("CEP incompleto ou inválido: {}", cep);
                    return ResponseEntity.notFound().build();
                }
            }
            
            log.debug("Endereço encontrado para CEP: {}", cep);
            return ResponseEntity.ok(response.getBody());
            
        } catch (Exception e) {
            log.error("Erro ao buscar CEP: {}", cep, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Erro ao consultar CEP. Tente novamente."));
        }
    }
}
