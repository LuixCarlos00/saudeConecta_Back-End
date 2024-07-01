package br.com.saudeConecta.infra.configuracoesseguranca;

import br.com.saudeConecta.infra.configuracoesseguranca.filtroSeguranca.FiltroAcesso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Autowired
    private FiltroAcesso filtroAcesso;

    @Value("${url_Front_End}")
    private String url_Front_End;

    @Value("${url_Back_End}")
    private String url_Back_End;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.cors().and().csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/Home/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/Home/cadastralogin").permitAll()
                .requestMatchers(HttpMethod.GET,"/Home/buscarUsuarioExistente/{login}").permitAll()
                .requestMatchers(HttpMethod.GET, "/paciente/buscarPorEmail/{email}").permitAll()
                .requestMatchers(HttpMethod.GET, "/medico/buscarPorEmail/{email}").permitAll()
                .requestMatchers(HttpMethod.GET, "/paciente/InserirCodigo/{codigo}").permitAll()
                .requestMatchers(HttpMethod.PUT, "/Home/trocaDeSenha/{Id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/Home/recuperaLogin={Id}&dados={tipoUsuario}").permitAll()
                .requestMatchers(HttpMethod.GET,"/administrador/buscarPorCoigoAutorizacao/{codigo}").permitAll()
                .anyRequest().authenticated()
                .and()
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

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(url_Front_End, url_Back_End)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
