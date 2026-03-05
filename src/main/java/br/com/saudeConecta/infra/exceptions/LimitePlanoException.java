package br.com.saudeConecta.infra.exceptions;

public class LimitePlanoException extends RuntimeException {

    private final String role;
    private final Integer limite;
    private final Long usado;

    public LimitePlanoException(String message) {
        super(message);
        this.role = null;
        this.limite = null;
        this.usado = null;
    }

    /**
     * Lança exceção informando o limite excedido para uma role específica.
     *
     * @param role   nome da role (ex: "PROFISSIONAL")
     * @param limite limite máximo do plano
     * @param usado  quantidade já utilizada
     */
    public LimitePlanoException(String role, Integer limite, Long usado) {
        super(String.format(
                "Limite do plano atingido para %s: máximo %d, atual %d",
                role, limite, usado));
        this.role = role;
        this.limite = limite;
        this.usado = usado;
    }

    public String getRole() {
        return role;
    }

    public Integer getLimite() {
        return limite;
    }

    public Long getUsado() {
        return usado;
    }
}
