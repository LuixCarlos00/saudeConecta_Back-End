package br.com.saudeConecta.application.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private static final String CODIGO_ERRO_NEGOCIO = "BUSINESS_ERROR";

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = CODIGO_ERRO_NEGOCIO;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = CODIGO_ERRO_NEGOCIO;
    }

    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    /**
     * Erro de negócio/integração com status próprio, preservando a causa para o log.
     *
     * @param message mensagem para o cliente
     * @param status  status HTTP da resposta
     * @param cause   exceção original
     */
    public BusinessException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = CODIGO_ERRO_NEGOCIO;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = CODIGO_ERRO_NEGOCIO;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
