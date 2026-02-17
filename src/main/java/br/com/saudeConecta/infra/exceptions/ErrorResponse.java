package br.com.saudeConecta.infra.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String errorCode,
        List<FieldError> fieldErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null, null);
    }

    public ErrorResponse(int status, String error, String message, String path, String errorCode) {
        this(LocalDateTime.now(), status, error, message, path, errorCode, null);
    }

    public ErrorResponse(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        this(LocalDateTime.now(), status, error, message, path, null, fieldErrors);
    }

    public record FieldError(String field, String message) {}
}
