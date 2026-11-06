package br.com.saudeConecta.application.auth;

/**
 * Resultado de uma tentativa de login que passou pela verificação de senha: ou um token JWT, ou
 * uma recusa com a mensagem para o usuário. Falha de credencial ou usuário bloqueado não chega
 * aqui: sai como {@code AuthenticationException} (401 no handler global).
 *
 * @param token    token JWT; null quando recusado
 * @param recusa   motivo da recusa; null quando autenticado
 * @param mensagem mensagem da recusa para o usuário; null quando autenticado
 */
public record ResultadoLogin(String token, Recusa recusa, String mensagem) {

    /**
     * Garante que o resultado é um token OU uma recusa com mensagem, nunca os dois nem nenhum:
     * o controller monta {@code Map.of("message", mensagem)}, que estoura com mensagem nula.
     */
    public ResultadoLogin {
        boolean temToken = token != null;
        boolean temRecusa = recusa != null && mensagem != null;
        if (temToken == temRecusa || (temToken && (recusa != null || mensagem != null))) {
            throw new IllegalArgumentException("ResultadoLogin precisa ter só token ou só recusa com mensagem");
        }
    }

    /** Por que o login foi recusado; o controller traduz em status HTTP. */
    public enum Recusa {
        /** Perfil escolhido na tela diferente do tipo do usuário (403). */
        TIPO_DIVERGENTE,
        /** Perfil escolhido na tela não existe (400). */
        TIPO_INVALIDO,
        /** Clínica sem assinatura vigente ou com plano que não permite acesso, como CANCELADA (403). */
        PLANO_SEM_ACESSO
    }

    /**
     * @param token token JWT gerado
     * @return resultado de login aceito
     */
    public static ResultadoLogin autenticado(String token) {
        return new ResultadoLogin(token, null, null);
    }

    /**
     * @param recusa   motivo
     * @param mensagem mensagem para o usuário
     * @return resultado de login recusado
     */
    public static ResultadoLogin recusado(Recusa recusa, String mensagem) {
        return new ResultadoLogin(null, recusa, mensagem);
    }

    /** @return true quando há token */
    public boolean isAutenticado() {
        return token != null && recusa == null;
    }
}
