package br.com.saudeConecta.config.logging;

/**
 * Chaves do MDC usadas no logging.pattern (%X{requestId} %X{tenantId} %X{userId}).
 */
public final class MdcChaves {

    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";

    private MdcChaves() {
    }
}
