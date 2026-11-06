package br.com.saudeConecta.presentation.erro;

import br.com.saudeConecta.application.exception.BusinessException;
import br.com.saudeConecta.application.exception.LimitePlanoException;
import br.com.saudeConecta.application.exception.ResourceNotFoundException;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantNotDefinedException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Único tratador de exceções da API. Converte exceções em {@link ErrorResponse} com status coerente.
 *
 * <p>Regras: erro do cliente (4xx) é logado em WARN sem stacktrace; todo 500 é logado em ERROR com
 * stacktrace. Mensagens de banco/infra nunca vão para o corpo da resposta.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    static final String MSG_ERRO_INTERNO = "Erro interno do servidor";
    static final String MSG_INTEGRIDADE = "Operacao conflita com dados ja existentes ou vinculados";
    static final String MSG_CONCORRENCIA = "Registro alterado por outra operacao, recarregue e tente novamente";
    static final String MSG_NAO_ENCONTRADO = "Recurso nao encontrado";

    @ExceptionHandler(TenantNotDefinedException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotDefined(TenantNotDefinedException ex, HttpServletRequest req) {
        return cliente(HttpStatus.FORBIDDEN, "TENANT_NAO_DEFINIDO", ex.getMessage(), req, ex);
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccessDenied(TenantAccessDeniedException ex, HttpServletRequest req) {
        return cliente(HttpStatus.FORBIDDEN, "TENANT_ACESSO_NEGADO", ex.getMessage(), req, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return cliente(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Voce nao tem permissao para acessar este recurso", req, ex);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        String mensagem = ex instanceof BadCredentialsException
                ? "Credenciais invalidas"
                : "Usuario bloqueado ou inativo";
        return cliente(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", mensagem, req, ex);
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ErrorResponse> handleJwt(JWTVerificationException ex, HttpServletRequest req) {
        return cliente(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "Token invalido ou expirado. Faca login novamente.", req, ex);
    }

    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        return cliente(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagemDeNaoEncontrado(ex), req, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return cliente(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), req, ex);
    }

    /** Regra de negócio violada (transição inválida, conflito de horário, edição bloqueada...). */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return cliente(HttpStatus.CONFLICT, "CONFLITO", ex.getMessage(), req, ex);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        return cliente(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), req, ex);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return cliente(HttpStatus.CONFLICT, "CONCORRENCIA", MSG_CONCORRENCIA, req, ex);
    }

    @ExceptionHandler(LimitePlanoException.class)
    public ResponseEntity<ErrorResponse> handleLimitePlano(LimitePlanoException ex, HttpServletRequest req) {
        return cliente(HttpStatus.UNPROCESSABLE_ENTITY, "LIMITE_PLANO", ex.getMessage(), req, ex);
    }

    /**
     * A mensagem fica com o texto dos campos (o front mostra {@code message} primeiro); o mapa
     * campo → mensagem vai em {@code errors} para quem quiser destacar o campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        String mensagem = erros.isEmpty()
                ? "Erro de validacao nos campos"
                : String.join("; ", erros.values());
        log.warn("http.erro negado motivo=VALIDACAO metodo={} rota={} campos={}", req.getMethod(), rota(req), erros.keySet());
        return ResponseEntity.badRequest().body(ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(), "VALIDACAO", mensagem, req.getRequestURI(), erros));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return cliente(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "Valor invalido para o parametro: " + ex.getName(), req, ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return cliente(HttpStatus.BAD_REQUEST, "JSON_INVALIDO", "Corpo da requisicao ausente ou invalido", req, ex);
    }

    /** Violação de constraint: a mensagem do banco (tabela, índice, valor) fica só no log. */
    @ExceptionHandler({DataIntegrityViolationException.class, SQLException.class})
    public ResponseEntity<ErrorResponse> handleIntegridade(Exception ex, HttpServletRequest req) {
        log.warn("http.erro negado motivo=INTEGRIDADE metodo={} rota={}", req.getMethod(), rota(req), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(HttpStatus.CONFLICT.value(), "INTEGRIDADE", MSG_INTEGRIDADE, req.getRequestURI()));
    }

    /**
     * Último recurso. Exceções do próprio Spring MVC (405, 415, parâmetro ausente...) e as anotadas
     * com {@code @ResponseStatus} mantêm o status delas; o resto é 500 logado com stacktrace.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        HttpStatusCode statusConhecido = statusDeclarado(ex);
        if (statusConhecido != null && statusConhecido.is4xxClientError()) {
            return cliente(HttpStatus.valueOf(statusConhecido.value()), "REQUISICAO_INVALIDA",
                    HttpStatus.valueOf(statusConhecido.value()).getReasonPhrase(), req, ex);
        }
        log.error("http.erro falha metodo={} rota={}", req.getMethod(), rota(req), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "ERRO_INTERNO", MSG_ERRO_INTERNO, req.getRequestURI()));
    }

    private static HttpStatusCode statusDeclarado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            return er.getStatusCode();
        }
        ResponseStatus anotacao = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        return anotacao != null ? anotacao.code() : null;
    }

    /** Mensagens do Hibernate ("Unable to find br.com...Entidade with id 5") expõem o modelo interno. */
    private static String mensagemDeNaoEncontrado(RuntimeException ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank() || msg.startsWith("Unable to find") || ex instanceof NoSuchElementException) {
            return MSG_NAO_ENCONTRADO;
        }
        return msg;
    }

    /**
     * Rota para log: o padrão do mapeamento ("/publico/questionario/{token}") em vez da URI real,
     * para não gravar token de link público, CPF ou e-mail que vêm no path.
     */
    private static String rota(HttpServletRequest req) {
        Object padrao = req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return padrao != null ? padrao.toString() : req.getRequestURI();
    }

    private static ResponseEntity<ErrorResponse> cliente(HttpStatusCode status, String code, String mensagem,
                                                         HttpServletRequest req, Exception ex) {
        String texto = (mensagem == null || mensagem.isBlank())
                ? HttpStatus.valueOf(status.value()).getReasonPhrase()
                : mensagem;
        log.warn("http.erro negado status={} metodo={} rota={} motivo={} causa={}", status.value(), req.getMethod(), rota(req),
                texto, ex.getClass().getSimpleName());
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), code, texto, req.getRequestURI()));
    }
}
