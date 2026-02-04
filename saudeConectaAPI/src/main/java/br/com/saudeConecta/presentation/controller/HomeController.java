package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.HomeService;
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

import java.util.Map;

@RestController
@RequestMapping("/Home")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Description("Endpoints para autenticação principal do sistema")
public class HomeController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final HomeService recuperacaoSenhaService;

    @PostMapping("/login")
    @Description("Realiza autenticação do usuário e retorna token JWT. Utilizado em: LoginComponent, AuthService")
    public ResponseEntity<DadosTokenJWT> autenticar(@RequestBody @NotNull DadosLoginUsuario dados) {
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);

        var TokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(TokenJWT));
    }

    @PostMapping("/esqueciMinhaSenha")
    @Description("Recupera senha do usuário enviando nova senha por email")
    public ResponseEntity<?> esqueciMinhaSenha(@RequestBody Map<String, String> dados) {
        String email = dados.get("email");
        log.info("Solicitação de recuperação de senha para email: {}", email);

        try {
            recuperacaoSenhaService.recuperarSenhaPorEmail(email);
            return ResponseEntity.ok(Map.of("message", "Nova senha enviada para o email cadastrado"));
        } catch (HomeService.EmailNaoEncontradoException e) {
            log.warn("Email não encontrado: {}", email);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao recuperar senha: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Erro ao processar solicitação"));
        }
    }
}
