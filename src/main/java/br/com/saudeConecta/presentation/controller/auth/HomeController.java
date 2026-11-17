package br.com.saudeConecta.presentation.controller.auth;

import br.com.saudeConecta.application.auth.AutenticacaoService;
import br.com.saudeConecta.application.auth.RecuperacaoSenhaService;
import br.com.saudeConecta.application.auth.dto.DadosRedefinirSenha;
import br.com.saudeConecta.application.auth.dto.DadosValidarTokenRecuperacao;
import br.com.saudeConecta.application.auth.ResultadoLogin;
import br.com.saudeConecta.application.usuario.dto.DadosLoginUsuario;
import br.com.saudeConecta.application.usuario.dto.DadosTokenJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Entrada HTTP do login e da recuperação de senha. A regra do login (senha master auditada, perfil,
 * plano da clínica) mora em {@link AutenticacaoService} e a do link de redefinição em
 * {@link RecuperacaoSenhaService}; aqui só se traduz o resultado em status e corpo.
 */
@RestController
@RequestMapping("/Home")
@RequiredArgsConstructor
@Description("Endpoints para autenticação principal do sistema")
public class HomeController {

    private static final String CHAVE_MENSAGEM = "message";

    private final AutenticacaoService autenticacaoService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

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
     * Envia por e-mail um link de redefinição para cada conta com o e-mail. Não troca a senha.
     * Responde SEMPRE 200 com a mesma mensagem, exista ou não o e-mail, para não permitir
     * enumeração de contas. Falha inesperada sobe para o handler global (500).
     *
     * @param dados corpo com o campo "email"
     * @return 200 com mensagem genérica, ou 400 quando o e-mail não foi informado
     */
    @PostMapping("/esqueciMinhaSenha")
    @Description("Envia link de redefinição de senha por email. Utilizado em: RecuperaCadastroComponent")
    public ResponseEntity<Map<String, String>> esqueciMinhaSenha(@RequestBody Map<String, String> dados) {
        String email = dados.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(CHAVE_MENSAGEM, "Informe o e-mail"));
        }

        recuperacaoSenhaService.solicitar(email);
        return ResponseEntity.ok(Map.of(CHAVE_MENSAGEM, MENSAGEM_RECUPERACAO_GENERICA));
    }

    /**
     * Confere se o link de redefinição ainda vale, antes de a tela mostrar o formulário.
     *
     * @param dados corpo com o token do link
     * @return 200 quando vale; 400 com code TOKEN_RECUPERACAO_INVALIDO (handler global) quando
     *         expirou, já foi usado, foi substituído por um pedido novo ou não existe
     */
    @PostMapping("/redefinirSenha/validar")
    @Description("Valida o token do link de redefinição de senha. Utilizado em: RedefinirSenhaComponent")
    public ResponseEntity<Map<String, String>> validarLinkRedefinicao(@RequestBody @Valid DadosValidarTokenRecuperacao dados) {
        recuperacaoSenhaService.validar(dados.token());
        return ResponseEntity.ok(Map.of(CHAVE_MENSAGEM, "Link válido"));
    }

    /**
     * Grava a nova senha escolhida pelo dono do link e consome o token.
     *
     * @param dados corpo com o token e a nova senha
     * @return 200 com mensagem de sucesso; 400 com code TOKEN_RECUPERACAO_INVALIDO quando o link
     *         não vale, ou code VALIDACAO quando a senha não tem entre 8 e 100 caracteres
     */
    @PostMapping("/redefinirSenha")
    @Description("Redefine a senha pelo link enviado por email. Utilizado em: RedefinirSenhaComponent")
    public ResponseEntity<Map<String, String>> redefinirSenha(@RequestBody @Valid DadosRedefinirSenha dados) {
        recuperacaoSenhaService.redefinir(dados.token(), dados.novaSenha());
        return ResponseEntity.ok(Map.of(CHAVE_MENSAGEM, "Senha redefinida com sucesso. Faça login com a nova senha."));
    }
}
