package br.com.saudeConecta.presentation.controller;

import br.com.saudeConecta.application.service.HomeService;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.configuracoesseguranca.TokenService;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.ProfissionalRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
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
import java.util.Optional;

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
    private final ProfissionalRepository profissionalRepository;
    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final SecretariaRepository secretariaRepository;

    @PostMapping("/login")
    @Description("Realiza autenticação do usuário e retorna token JWT. Utilizado em: LoginComponent, AuthService")
    public ResponseEntity<DadosTokenJWT> autenticar(@RequestBody @NotNull DadosLoginUsuario dados) {
        var authenticatetoken = new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        var authentication = authenticationManager.authenticate(authenticatetoken);
        
        Usuario usuario = (Usuario) authentication.getPrincipal();
        Long organizacaoId = usuario.getOrganizacaoId();
        
        // Obter nome do usuário baseado no tipo
        String nomeUsuario = getNomeUsuario(usuario);
        
        String tokenJWT = tokenService.gerarToken(usuario, organizacaoId, nomeUsuario);
        log.info("Login realizado: {} | Org: {} | Nome: {}", usuario.getLogin(), organizacaoId, nomeUsuario);

        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
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

    private String getNomeUsuario(Usuario usuario) {
        try {
            // Busca baseada no tipo de usuário
            if (usuario.isAdminOrganizacao()) {
                Optional<AdminOrganizacao> admin = adminOrganizacaoRepository.findByUsuario_Id(usuario.getId());
                return admin.map(AdminOrganizacao::getNome).orElse("Administrador");
            } else if (usuario.isProfissional()) {
                Optional<Profissional> profissional = profissionalRepository.findByUsuario_Id(usuario.getId());
                return profissional.map(Profissional::getNome).orElse("Profissional");
            } else if (usuario.getTipoUsuario() != null && usuario.getTipoUsuario() == 3) { // Secretaria
                Optional<Secretaria> secretaria = secretariaRepository.findByUsuario_Id(usuario.getId());
                return secretaria.map(Secretaria::getNome).orElse("Secretária");
            }
        } catch (Exception e) {
            log.warn("Erro ao obter nome do usuário ID: {}", usuario.getId(), e);
        }
        
        return "Usuário"; // Default fallback
    }
}
