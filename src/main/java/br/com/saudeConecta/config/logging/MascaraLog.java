package br.com.saudeConecta.config.logging;

/**
 * Máscaras para dados pessoais (LGPD) e credenciais que aparecem em log.
 * Mantém só o suficiente para correlacionar/diagnosticar, nunca o valor inteiro.
 */
public final class MascaraLog {

    private static final int PREFIXO_TOKEN_VISIVEL = 6;
    private static final int TAMANHO_MAXIMO_SEGURO = 32;

    private MascaraLog() {
    }

    /**
     * Mascara tokens de links públicos, assinaturas e afins. O token inteiro funciona como
     * credencial (ex.: acesso ao dado de saúde do paciente), então nunca vai completo para o log.
     *
     * @param token valor sensível (pode ser nulo)
     * @return os 6 primeiros caracteres + "***", "***" quando curto demais, ou "null" quando nulo
     */
    public static String token(String token) {
        if (token == null) {
            return "null";
        }
        if (token.length() <= PREFIXO_TOKEN_VISIVEL) {
            return "***";
        }
        return token.substring(0, PREFIXO_TOKEN_VISIVEL) + "***";
    }

    /**
     * Mascara e-mail mantendo a primeira letra e o domínio ("fulano@clinica.com" vira "f***@clinica.com").
     *
     * @param email e-mail (pode ser nulo)
     * @return e-mail mascarado, "&lt;vazio&gt;" quando nulo/vazio ou "***" quando não parece e-mail
     */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "<vazio>";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    /**
     * Mascara CPF mostrando só os 2 dígitos verificadores ("123.456.789-01" vira "***.***.***-01").
     *
     * @param cpf CPF com ou sem pontuação (pode ser nulo)
     * @return CPF mascarado, "***" quando curto demais, ou "null" quando nulo
     */
    public static String cpf(String cpf) {
        if (cpf == null) {
            return "null";
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() < 2) {
            return "***";
        }
        return "***.***.***-" + digitos.substring(digitos.length() - 2);
    }

    /**
     * Mascara CEP (dado de endereço) mantendo os 5 primeiros dígitos ("01310-100" vira "01310***").
     *
     * @param cep CEP com ou sem hífen (pode ser nulo)
     * @return 5 primeiros dígitos + "***", "***" quando tem menos de 5 dígitos, ou "null" quando nulo
     */
    public static String cep(String cep) {
        if (cep == null) {
            return "null";
        }
        String digitos = cep.replaceAll("\\D", "");
        if (digitos.length() < 5) {
            return "***";
        }
        return digitos.substring(0, 5) + "***";
    }

    /**
     * Neutraliza texto controlado pelo cliente (header, parâmetro) antes de ir para o log: troca por
     * {@code _} qualquer caractere fora de {@code [0-9A-Za-z._:-]} (inclusive CR/LF, que permitiriam
     * forjar linhas de log) e limita a {@value #TAMANHO_MAXIMO_SEGURO} caracteres.
     *
     * @param valor texto cru (pode ser nulo)
     * @return texto sanitizado e truncado, ou "null" quando nulo
     */
    public static String seguro(String valor) {
        return seguro(valor, TAMANHO_MAXIMO_SEGURO);
    }

    /**
     * Igual a {@link #seguro(String)}, com limite de tamanho informado (para mensagens mais longas).
     *
     * @param valor texto cru (pode ser nulo)
     * @param maximo quantidade máxima de caracteres devolvidos
     * @return texto sanitizado e truncado, ou "null" quando nulo
     */
    public static String seguro(String valor, int maximo) {
        if (valor == null) {
            return "null";
        }
        String limpo = valor.replaceAll("[^0-9A-Za-z._:-]", "_");
        return limpo.length() > maximo ? limpo.substring(0, maximo) : limpo;
    }
}
