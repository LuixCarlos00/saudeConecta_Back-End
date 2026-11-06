package br.com.saudeConecta.config.tenant;

public class TenantNotDefinedException extends RuntimeException {
    
    public TenantNotDefinedException(String message) {
        super(message);
    }
    
    public TenantNotDefinedException(String message, Throwable cause) {
        super(message, cause);
    }
}
