package br.com.saudeConecta.presentation.controller.auth;

import br.com.saudeConecta.application.auth.AutenticacaoService;
import br.com.saudeConecta.application.auth.ResultadoLogin;
import br.com.saudeConecta.application.usuario.dto.DadosLoginUsuario;
import br.com.saudeConecta.application.usuario.dto.DadosTokenJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Entrada HTTP do login e da recuperação de senha. A regra (senha master auditada, perfil, plano
 * da clínica e resposta neutra) mora em {@link AutenticacaoService}; aqui só se traduz o
 * resultado em status e corpo.
 */
@RestController
@RequestMapping("/Home")
@RequiredArgsConstructor
@Description("Endpoints para autenticação principal do sistema")
public class HomeController {

    private static final String CHAVE_MENSAGEM = "message";

    private final AutenticacaoService autenticacaoService;

    /** Mesma resposta para e-mail cadastrado ou não: impede descobrir quais e-mails existem. */
    static final String MENSAGEM_RECUPERACAO_GENERICA =
            "Se o e-mail estiver cadastrado, voce recebera as instrucoes para recuperar a senha.";

    /**
     * Autentica e devolve o token JWT.
     *
     * @param dados   login, senha e perfil escolhido na tela
     * @param request requisição (o IP vai para a auditoria da senha master)
     * @return 200 com o token; 403 (perfil divergente ou plano sem acesso) ou 400 (perfil inválido)
     *         com {@code {"message": ...}}; credencial inválida ou usuário bloqueado saem 401 pelo handler global
     */
    @PostMapping("/login")
    @Description("Realiza autenticação do usuário e retorna token JWT. Utilizado em: LoginComponent, AuthService")
    public ResponseEntity<Object> autenticar(@RequestBody @NotNull DadosLoginUsuario dados, HttpServletRequest request) {
        ResultadoLogin resultado = autenticacaoService.autenticar(dados, request.getRemoteAddr());
        if (resultado.isAutenticado()) {
            return ResponseEntity.ok(new DadosTokenJWT(resultado.token()));
        }
        HttpStatus status = resultado.recusa() == ResultadoLogin.Recusa.TIPO_INVALIDO
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(Map.of(CHAVE_MENSAGEM, resultado.mensagem()));
    }

    /**
     * Gera nova senha e envia por e-mail. Responde SEMPRE 200 com a mesma mensagem, exista ou não o
     * e-mail, para não permitir enumeração de contas. Falha inesperada sobe para o handler global (500).
     *
     * @param dados corpo com o campo "email"
     * @return 200 com mensagem genérica, ou 400 quando o e-mail não foi informado
     */
    @PostMapping("/esqueciMinhaSenha")
    @Description("Recupera senha do usuário enviando nova senha por email")
    public ResponseEntity<Map<String, String>> esqueciMinhaSenha(@RequestBody Map<String, String> dados) {
        String email = dados.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(CHAVE_MENSAGEM, "Informe o e-mail"));
        }

        autenticacaoService.solicitarRecuperacaoSenha(email);
        return ResponseEntity.ok(Map.of(CHAVE_MENSAGEM, MENSAGEM_RECUPERACAO_GENERICA));
    }
}
