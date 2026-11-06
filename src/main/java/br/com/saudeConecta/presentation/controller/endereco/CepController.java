package br.com.saudeConecta.presentation.controller.endereco;

import br.com.saudeConecta.application.endereco.port.BuscaCep;
import br.com.saudeConecta.application.endereco.port.CepIndisponivelException;
import br.com.saudeConecta.application.endereco.port.CepNaoEncontradoException;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.presentation.erro.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/cep")
@RequiredArgsConstructor
@Slf4j
public class CepController {

    private final BuscaCep buscaCep;

    /**
     * Busca endereço pelo CEP usando o serviço de CEP (ViaCEP) como proxy.
     *
     * @param cep CEP para busca (apenas números ou formatado)
     * @return 200 com os dados do endereço; 400 se não tiver 8 dígitos; 404 sem corpo se não
     *         existir; 502 {@code CEP_INDISPONIVEL} se o serviço externo falhar
     */
    @GetMapping("/{cep}")
    public ResponseEntity<Object> buscarEnderecoPorCep(@PathVariable String cep) {
        // Remove caracteres não numéricos do CEP
        String cepLimpo = cep.replaceAll("\\D", "");
        log.debug("cep.buscar inicio cep={}", MascaraLog.cep(cep));

        if (cepLimpo.length() != 8) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "CEP deve conter 8 dígitos"));
        }

        try {
            Object endereco = buscaCep.buscar(cepLimpo);
            log.debug("cep.buscar ok cep={}", MascaraLog.cep(cep));
            return ResponseEntity.ok(endereco);
        } catch (CepNaoEncontradoException e) {
            return ResponseEntity.notFound().build();
        } catch (CepIndisponivelException e) {
            // Falha do serviço externo (já registrada no adaptador): 502 com mensagem para o usuário
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(HttpStatus.BAD_GATEWAY.value(), "CEP_INDISPONIVEL",
                        "Servico de CEP indisponivel no momento. Tente novamente.", "/cep/" + cep));
        }
    }
}
