package br.com.saudeConecta.config.security;

import br.com.saudeConecta.config.tenant.TenantFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurations {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, FiltroAcesso filtroAcesso,
                                                   TenantFilter tenantFilter) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfigurationSource())).csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/Home/esqueciMinhaSenha").permitAll()
                .requestMatchers(HttpMethod.POST, "/Home/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/cep/**").permitAll()
                // Endpoints públicos explícitos — evita expor novos /publico/** por engano
                .requestMatchers(HttpMethod.GET, "/publico/questionario/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/publico/questionario/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/publico/planejamento/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/publico/planejamento/**").permitAll()
                // Webhook do Mercado Pago: autenticado por assinatura HMAC, não por JWT
                .requestMatchers(HttpMethod.POST, "/publico/webhooks/mercadopago").permitAll()
                // Auto-cadastro SaaS a partir da landing page
                .requestMatchers(HttpMethod.POST, "/publico/cadastro").permitAll()
                .requestMatchers(HttpMethod.GET, "/planos").permitAll()
                .requestMatchers(HttpMethod.GET, "/planos/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroAcesso, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * Única configuração de CORS da aplicação (antes havia uma aqui e outra num WebMvcConfigurer,
     * além de @CrossOrigin nos controllers). As origens vêm de {@code app.cors.allowed-origins},
     * lista separada por vírgula: em dev o front local, em produção a env CORS_ALLOWED_ORIGINS.
     *
     * @return fonte de configuração CORS aplicada a todas as rotas
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "ngrok-skip-browser-warning"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
