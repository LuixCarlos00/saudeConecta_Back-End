package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import br.com.saudeConecta.presentation.dto.usuario.DadosLoginUsuario;
import br.com.saudeConecta.presentation.dto.usuario.DadosTokenJWT;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Home")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description( "Endpoints para autenticação principal do sistema")
public class HomeController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping("/login")
    @Description("Realiza autenticação do usuário e retorna token JWT. Utilizado em: LoginComponent, AuthService")
    public ResponseEntity<DadosTokenJWT> autenticar(@RequestBody @NotNull DadosLoginUsuario dados) {
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        var TokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(TokenJWT));
    }

}
