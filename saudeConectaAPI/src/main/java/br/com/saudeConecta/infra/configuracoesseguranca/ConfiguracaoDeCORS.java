package br.com.saudeConecta.infra.configuracoesseguranca;


import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;


@Configuration
public class ConfiguracaoDeCORS {





    @Value("${url_LocalHost}")
    private String url_LocalHost;



    public void addCorsMappings(@NotNull @ NotNull CorsRegistry registry) {
        String origins = url_LocalHost ;

        registry.addMapping("/**")
                .allowedOrigins(origins )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization");
    }


}
