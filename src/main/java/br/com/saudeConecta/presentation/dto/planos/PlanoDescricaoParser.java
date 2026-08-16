package br.com.saudeConecta.presentation.dto.planos;

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
    private static final Pattern RECURSOS_PATTERN = Pattern.compile("Recursos:\\s*(\\[.*?\\])");

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

        log.debug("Parseando descrição: {}", descricao);

        // Extrair título
        Matcher tituloMatcher = TITULO_PATTERN.matcher(descricao);
        if (tituloMatcher.find()) {
            result.setTitulo(tituloMatcher.group(1).trim());
            log.debug("Título encontrado: {}", tituloMatcher.group(1).trim());
        } else {
            // Se não encontrar o padrão, usa a descrição inteira como título
            result.setTitulo(descricao.trim());
            log.debug("Padrão de título não encontrado, usando descrição completa");
        }

        // Extrair recursos (JSON array)
        Matcher recursosMatcher = RECURSOS_PATTERN.matcher(descricao);
        if (recursosMatcher.find()) {
            try {
                String recursosJson = recursosMatcher.group(1);
                log.debug("Recursos JSON encontrado: {}", recursosJson);
                List<String> recursos = objectMapper.readValue(recursosJson, new TypeReference<List<String>>() {});
                result.setRecursos(recursos);
                log.debug("Recursos parseados: {}", recursos);
            } catch (JsonProcessingException e) {
                log.warn("Erro ao fazer parse de recursos JSON: {}", e.getMessage());
                result.setRecursos(new ArrayList<>());
            }
        } else {
            log.debug("Padrão de recursos não encontrado");
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
            return "titulo: " + titulo.trim();
        }

        try {
            String recursosJson = objectMapper.writeValueAsString(recursos);
            return "titulo: " + titulo.trim() + "\nRecursos: " + recursosJson;
        } catch (JsonProcessingException e) {
            log.error("Erro ao converter recursos para JSON: {}", e.getMessage());
            return "titulo: " + titulo.trim();
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