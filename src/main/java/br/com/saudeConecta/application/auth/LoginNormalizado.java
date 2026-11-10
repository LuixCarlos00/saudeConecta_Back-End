package br.com.saudeConecta.application.auth;

import java.util.regex.Pattern;

/**
 * Normaliza o login digitado na tela. O login gravado é o CPF ou o CNPJ só com dígitos (o
 * cadastro tira a máscara), mas o usuário pode digitar com pontos, traço e barra.
 */
public final class LoginNormalizado {

    /** Só dígitos e os separadores das máscaras de CPF e CNPJ. */
    private static final Pattern FORMATO_DOCUMENTO = Pattern.compile("[0-9.\\-/ ]+");
    private static final int DIGITOS_CPF = 11;
    private static final int DIGITOS_CNPJ = 14;

    private LoginNormalizado() {
    }

    /**
     * Tira a máscara quando o login tem cara de CPF ou CNPJ; qualquer outro login (ex.: um
     * login antigo com letras) volta só sem os espaços das pontas.
     *
     * @param login login digitado (pode ser nulo)
     * @return CPF/CNPJ só com dígitos, o login sem espaços nas pontas, ou null
     */
    public static String normalizar(String login) {
        if (login == null) {
            return null;
        }
        String limpo = login.strip();
        if (!FORMATO_DOCUMENTO.matcher(limpo).matches()) {
            return limpo;
        }
        String digitos = limpo.replaceAll("\\D", "");
        return digitos.length() == DIGITOS_CPF || digitos.length() == DIGITOS_CNPJ ? digitos : limpo;
    }
}
