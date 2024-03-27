package br.com.saudeConecta.infra.exceptions;

public class EmailServiceException extends RuntimeException {
    public EmailServiceException(String message) {
        super(message);
    }
}
