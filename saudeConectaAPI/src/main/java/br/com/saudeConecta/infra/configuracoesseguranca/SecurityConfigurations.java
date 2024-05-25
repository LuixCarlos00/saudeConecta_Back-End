package br.com.saudeConecta.infra.configuracoesseguranca;



import br.com.saudeConecta.infra.configuracoesseguranca.filtroSeguranca.FiltroAcesso;
import jdk.jfr.Name;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import static org.springframework.security.config.Customizer.withDefaults;


@Configuration
@EnableWebSecurity
//@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class SecurityConfigurations {

    @Autowired
    private FiltroAcesso filtroAcesso;




    //Processo de Autenticação Stateless
    @Name("Bean injetado para poder o controle do modo STATELESS e também configura quais rotas estão disponíveis paa o usuário sem o uso do Token  ")
    @Bean
    public SecurityFilterChain securityFilterChain(@NotNull HttpSecurity http) throws Exception {


        return http.csrf().disable().cors(withDefaults())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/Home/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/Home/cadastralogin").permitAll()
                .requestMatchers(HttpMethod.GET, "/paciente/buscarPorEmail/{email}").permitAll()
                .requestMatchers(HttpMethod.GET, "/medico/buscarPorEmail/{email}").permitAll()
                .requestMatchers(HttpMethod.GET, "/paciente/InserirCodigo/{codigo}").permitAll()
                .requestMatchers(HttpMethod.PUT, "/Home/trocaDeSenha/{Id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/Home/recuperaLogin={Id}&dados={tipoUsuario}").permitAll()
                .requestMatchers(HttpMethod.GET, "/adm/buscarPorCoigoAutorizacao/{codigo}").permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(filtroAcesso, UsernamePasswordAuthenticationFilter.class)
                .build();



    }

    @Bean
    public AuthenticationManager authenticationManager(@NotNull AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }






}