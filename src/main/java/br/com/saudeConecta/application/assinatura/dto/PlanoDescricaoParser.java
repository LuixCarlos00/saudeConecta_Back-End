package br.com.saudeConecta.application.assinatura.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário para fazer parse e formatação da descrição dos planos.
 * Formato esperado: "titulo: <descrição>\nRecursos: [<recursos em JSON>]"
 */
@Slf4j
public class PlanoDescricaoParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern TITULO_PATTERN = Pattern.compile("titulo:\\s*(.+?)\\s*(?:Recursos:|$)");
    // Classe negada (em vez de ".*?") com quantificador possessivo: mesma semântica, sem backtracking.
    // Os terminadores de linha excluídos replicam o que o "." não casa.
    private static final Pattern RECURSOS_PATTERN = Pattern.compile("Recursos:\\s*(\\[[^\\]\\n\\r\\u0085\\u2028\\u2029]*+\\])");
    private static final String PREFIXO_TITULO = "titulo: ";

    private PlanoDescricaoParser() {
        // Classe utilitária: não deve ser instanciada
    }

    /**
     * Classe interna para representar a descrição parseada.
     */
    public static class PlanoDescricao {
        private String titulo;
        private List<String> recursos;

        public PlanoDescricao() {
            this.recursos = new ArrayList<>();
        }

        public PlanoDescricao(String titulo, List<String> recursos) {
            this.titulo = titulo;
            this.recursos = recursos != null ? recursos : new ArrayList<>();
        }

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        public List<String> getRecursos() {
            return recursos;
        }

        public void setRecursos(List<String> recursos) {
            this.recursos = recursos;
        }
    }

    /**
     * Faz parse da descrição no formato "titulo: xxx\nRecursos: [xxx, xxx]"
     *
     * @param descricao descrição completa do plano
     * @return PlanoDescricao com titulo e recursos parseados
     */
    public static PlanoDescricao parse(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            return new PlanoDescricao("", new ArrayList<>());
        }

        PlanoDescricao result = new PlanoDescricao();

        log.debug("plano.descricao parse descricao={}", descricao);

        // Extrair título
        Matcher tituloMatcher = TITULO_PATTERN.matcher(descricao);
        if (tituloMatcher.find()) {
            result.setTitulo(tituloMatcher.group(1).trim());
            log.debug("plano.descricao titulo={}", tituloMatcher.group(1).trim());
        } else {
            // Se não encontrar o padrão, usa a descrição inteira como título
            result.setTitulo(descricao.trim());
            log.debug("plano.descricao titulo nao encontrado acao=USA_DESCRICAO_COMPLETA");
        }

        // Extrair recursos (JSON array)
        Matcher recursosMatcher = RECURSOS_PATTERN.matcher(descricao);
        if (recursosMatcher.find()) {
            try {
                String recursosJson = recursosMatcher.group(1);
                log.debug("plano.descricao recursos json={}", recursosJson);
                List<String> recursos = objectMapper.readValue(recursosJson, new TypeReference<List<String>>() {});
                result.setRecursos(recursos);
                log.debug("plano.descricao recursos parseados={}", recursos);
            } catch (JsonProcessingException e) {
                log.warn("plano.descricao parse falha motivo=RECURSOS_JSON_INVALIDO causa={}", e.getClass().getSimpleName());
                result.setRecursos(new ArrayList<>());
            }
        } else {
            log.debug("plano.descricao recursos nao encontrados");
        }

        return result;
    }

    /**
     * Formata título e recursos no padrão esperado.
     *
     * @param titulo   título do plano
     * @param recursos lista de recursos
     * @return String formatada "titulo: xxx\nRecursos: [xxx, xxx]"
     */
    public static String format(String titulo, List<String> recursos) {
        if (titulo == null || titulo.trim().isEmpty()) {
            titulo = "";
        }

        if (recursos == null || recursos.isEmpty()) {
            return PREFIXO_TITULO + titulo.trim();
        }

        try {
            String recursosJson = objectMapper.writeValueAsString(recursos);
            return PREFIXO_TITULO + titulo.trim() + "\nRecursos: " + recursosJson;
        } catch (JsonProcessingException e) {
            log.warn("plano.descricao formatar falha causa={}", e.getClass().getSimpleName());
            return PREFIXO_TITULO + titulo.trim();
        }
    }

    /**
     * Formata a descrição para compatibilidade com versões antigas (sem recursos).
     *
     * @param descricao descrição simples (apenas texto)
     * @return String formatada "titulo: xxx\nRecursos: []"
     */
    public static String formatFromSimpleDescription(String descricao) {
        return format(descricao != null ? descricao : "", new ArrayList<>());
    }
}