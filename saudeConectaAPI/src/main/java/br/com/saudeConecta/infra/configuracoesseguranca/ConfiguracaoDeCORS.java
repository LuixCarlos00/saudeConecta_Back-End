package br.com.saudeConecta.infra.configuracoesseguranca;


import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;


@Configuration
public class ConfiguracaoDeCORS {





    @Value("${url_Front_End}")
    private String url_Front_End;

    @Value("${url_Back_End}")
    private String url_Back_End;




    public void addCorsMappings(@NotNull @ NotNull CorsRegistry registry) {
        String origins = url_Front_End;
        String origins2 = url_Back_End;

        registry.addMapping("/**")
                .allowedOrigins(origins , origins2)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization");
    }


}
