package br.com.saudeConecta.infra.configuracoesseguranca;


import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;


@Configuration
public class ConfiguracaoDeCORS {

    public void addCorsMappings(@NotNull @org.jetbrains.annotations.NotNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT")
                .allowedHeaders("*")
                .exposedHeaders("Authorization");
    }


}
