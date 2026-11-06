package br.com.saudeConecta.config.web;

import java.util.regex.Pattern;

/**
 * Utilitário para sanitização de inputs do usuário.
 * Previne injeções SQL, XSS e caracteres maliciosos em campos de texto.
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // Classe utilitária — não deve ser instanciada
    }

    // Padrões de detecção de SQL injection
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE|EXEC|EXECUTE|" +
        "TRUNCATE|DECLARE|CAST|CONVERT|WAITFOR|DELAY|BENCHMARK|SLEEP)\\b|--|;|/\\*|\\*/|" +
        "\\bOR\\b\\s+\\d+=\\d+|\\bAND\\b\\s+\\d+=\\d+|'\\s*(OR|AND)\\s+')"
    );

    // Padrões de detecção de XSS
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<\\s*script|<\\s*img|<\\s*iframe|<\\s*object|<\\s*embed|<\\s*link|" +
        "javascript\\s*:|on\\w+\\s*=|<\\s*svg|<\\s*math|<\\s*style|<\\s*form|" +
        "expression\\s*\\(|eval\\s*\\(|alert\\s*\\()"
    );

    /**
     * Verifica se o texto contém padrões de SQL injection.
     * @param input texto de entrada
     * @return true se contém padrões suspeitos
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null || input.isBlank()) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Verifica se o texto contém padrões de XSS.
     * @param input texto de entrada
     * @return true se contém padrões suspeitos
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isBlank()) return false;
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Verifica se o texto contém conteúdo malicioso (SQL injection ou XSS).
     * @param input texto de entrada
     * @return true se o conteúdo é seguro (não contém padrões maliciosos)
     */
    public static boolean isSafe(String input) {
        if (input == null || input.isBlank()) return true;
        return !containsSqlInjection(input) && !containsXss(input);
    }
}
