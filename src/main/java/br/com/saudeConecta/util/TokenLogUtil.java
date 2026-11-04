package br.com.saudeConecta.util;

/**
 * Mascaramento de tokens de links públicos (questionário, assinatura de planejamento) para log.
 * O token inteiro funciona como credencial de acesso ao dado de saúde do paciente,
 * então nunca deve aparecer completo em log.
 */
public final class TokenLogUtil {

    private static final int PREFIXO_VISIVEL = 6;

    private TokenLogUtil() {
    }

    /**
     * Mantém apenas os 6 primeiros caracteres do token, suficiente para correlacionar logs.
     *
     * @param token token público (pode ser nulo)
     * @return token mascarado, "***" quando curto demais, ou "null" quando nulo
     */
    public static String mascarar(String token) {
        if (token == null) {
            return "null";
        }
        if (token.length() <= PREFIXO_VISIVEL) {
            return "***";
        }
        return token.substring(0, PREFIXO_VISIVEL) + "***";
    }
}
