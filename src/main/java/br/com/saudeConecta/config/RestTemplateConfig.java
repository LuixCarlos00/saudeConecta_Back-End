package br.com.saudeConecta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate com timeouts. Sem eles, um Mercado Pago travado prende a thread do
     * scheduler (e o row lock aberto pela régua/reconciliação) indefinidamente.
     *
     * @param connectTimeout tempo máximo para abrir a conexão (app.http.connect-timeout, padrão 5s)
     * @param readTimeout    tempo máximo de espera pela resposta (app.http.read-timeout, padrão 15s)
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate restTemplate(@Value("${app.http.connect-timeout:5s}") Duration connectTimeout,
                                     @Value("${app.http.read-timeout:15s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return new RestTemplate(factory);
    }
}
