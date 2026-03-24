package br.com.saudeConecta.util;

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

    // Padrão para CPF (apenas dígitos, pontos e hífen)
    private static final Pattern CPF_PATTERN = Pattern.compile("^[\\d.\\-]+$");

    // Padrão para CNPJ (apenas dígitos, pontos, barra e hífen)
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^[\\d.\\-/]+$");

    // Padrão para CEP (apenas dígitos e hífen)
    private static final Pattern CEP_PATTERN = Pattern.compile("^[\\d\\-]+$");

    // Padrão para telefone (dígitos, parênteses, espaço e hífen)
    private static final Pattern TELEFONE_PATTERN = Pattern.compile("^[\\d()\\s\\-+]+$");

    /**
     * Sanitiza texto genérico removendo tags HTML e caracteres perigosos.
     * @param input texto de entrada
     * @return texto sanitizado ou null se input for null
     */
    public static String sanitizeText(String input) {
        if (input == null) return null;
        String sanitized = input.trim();
        if (sanitized.isEmpty()) return sanitized;

        // Remove tags HTML
        sanitized = sanitized.replaceAll("<[^>]*>", "");
        // Remove caracteres de controle (exceto newline e tab)
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        return sanitized;
    }

    /**
     * Sanitiza um nome (apenas letras, espaços e acentos).
     * @param nome nome de entrada
     * @return nome sanitizado
     */
    public static String sanitizeName(String nome) {
        if (nome == null) return null;
        String sanitized = sanitizeText(nome);
        // Remove tudo que não seja letra, espaço, acento ou apóstrofo
        return sanitized.replaceAll("[^\\p{L}\\s'\\-]", "").trim();
    }

    /**
     * Limpa CPF removendo formatação (pontos e hífens).
     * @param cpf CPF formatado ou não
     * @return apenas os dígitos do CPF ou null
     */
    public static String sanitizeCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^\\d]", "");
    }

    /**
     * Limpa CNPJ removendo formatação.
     * @param cnpj CNPJ formatado ou não
     * @return apenas os dígitos do CNPJ ou null
     */
    public static String sanitizeCnpj(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("[^\\d]", "");
    }

    /**
     * Limpa CEP removendo formatação.
     * @param cep CEP formatado ou não
     * @return apenas os dígitos do CEP ou null
     */
    public static String sanitizeCep(String cep) {
        if (cep == null) return null;
        return cep.replaceAll("[^\\d]", "");
    }

    /**
     * Limpa telefone removendo formatação.
     * @param telefone telefone formatado ou não
     * @return apenas os dígitos do telefone ou null
     */
    public static String sanitizeTelefone(String telefone) {
        if (telefone == null) return null;
        return telefone.replaceAll("[^\\d]", "");
    }

    /**
     * Sanitiza email (trim + lowercase).
     * @param email email de entrada
     * @return email sanitizado ou null
     */
    public static String sanitizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

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

    /**
     * Valida se um CPF tem formato válido (apenas dígitos após limpeza).
     * @param cpf CPF limpo
     * @return true se válido
     */
    public static boolean isValidCpfFormat(String cpf) {
        if (cpf == null) return false;
        String cleaned = sanitizeCpf(cpf);
        return cleaned.length() == 11 && cleaned.chars().allMatch(Character::isDigit);
    }

    /**
     * Valida se um CNPJ tem formato válido (apenas dígitos após limpeza).
     * @param cnpj CNPJ limpo
     * @return true se válido
     */
    public static boolean isValidCnpjFormat(String cnpj) {
        if (cnpj == null) return false;
        String cleaned = sanitizeCnpj(cnpj);
        return cleaned.length() == 14 && cleaned.chars().allMatch(Character::isDigit);
    }
}
