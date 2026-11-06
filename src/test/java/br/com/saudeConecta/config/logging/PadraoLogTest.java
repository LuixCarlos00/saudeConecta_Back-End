package br.com.saudeConecta.config.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.fail;

/**
 * Guarda do padrão de logs descrito em {@code docs/LOGGING.md}. Lê todos os {@code .java} de
 * {@code src/main/java} e falha, listando arquivo e linha, quando encontra uma chamada {@code log.*}
 * que viola uma destas regras:
 *
 * <ol>
 *   <li><b>(a)</b> {@code getMessage()} em qualquer posição quando o último argumento é uma variável de
 *       exceção típica ({@code e}, {@code ex}, {@code erro}, {@code exception}, {@code t}), como em
 *       {@code log.error("x: {}", e.getMessage(), e)}: a mensagem já vem no stack trace.</li>
 *   <li><b>(b)</b> mensagem de log contendo {@code ===}.</li>
 *   <li><b>(c)</b> placeholder cuja chave <i>contém</i> dado pessoal ({@code email}, {@code cpf},
 *       {@code token}, {@code login}, {@code telefone}, {@code destinatario}; ex.: {@code emailAdmin={}},
 *       {@code destinatario={}}) e cujo argumento correspondente não passa por {@code MascaraLog.} na mesma chamada.</li>
 * </ol>
 *
 * <p><b>Heurística:</b> não há parser Java. Cada chamada é recortada do texto, do {@code log.nivel(} até o
 * parêntese que o fecha, respeitando aspas, caracteres e parênteses aninhados. Os argumentos são separados
 * por vírgula de nível zero. O primeiro é a mensagem (literais concatenados com {@code +} são unidos) e
 * o k-ésimo {@code {}} da mensagem corresponde ao k-ésimo argumento seguinte. A regra (a) olha o texto
 * bruto dos argumentos. A regra (c) reconhece a chave pelo trecho que antecede o {@code =}, sem diferenciar
 * maiúsculas de minúsculas; o argumento é aceito se contém {@code MascaraLog.}. Comentários ({@code //} e
 * {@code /* *}{@code /}) são trocados por espaços antes da análise, preservando as linhas; blocos de texto
 * ({@code """}) não são tratados.</p>
 */
class PadraoLogTest {

    private static final Path RAIZ_FONTES = Paths.get("src", "main", "java");

    private static final Pattern INICIO_CHAMADA = Pattern.compile("\\blog\\.(?:debug|info|warn|error|trace)\\(");
    private static final Pattern VARIAVEL_DE_EXCECAO =
            Pattern.compile("e|ex|erro|exception|t");
    private static final Pattern PLACEHOLDER_SENSIVEL =
            // Usado com find(): o prefixo do nome não importa, e o sufixo possessivo evita backtracking (S8786)
            Pattern.compile("(?:email|cpf|token|login|telefone|destinatario)\\w*+=\\{}",
                    Pattern.CASE_INSENSITIVE);
    private static final int JANELA_CHAVE = 40;

    @Test
    @DisplayName("Logs de produção seguem o padrão: sem getMessage() duplicado, sem '===' e dados pessoais mascarados")
    void logsSeguemOPadrao() throws IOException {
        List<String> violacoes = new ArrayList<>();

        try (Stream<Path> arquivos = Files.walk(RAIZ_FONTES)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                verificar(arquivo, semComentarios(Files.readString(arquivo)), violacoes);
            }
        }

        if (!violacoes.isEmpty()) {
            fail("Violações do padrão de logs (docs/LOGGING.md):\n" + String.join("\n", violacoes));
        }
    }

    private void verificar(Path arquivo, String fonte, List<String> violacoes) {
        Matcher inicio = INICIO_CHAMADA.matcher(fonte);
        while (inicio.find()) {
            int abre = inicio.end() - 1;
            int fecha = encontrarFechamento(fonte, abre);
            if (fecha < 0) {
                continue;
            }
            String chamada = fonte.substring(inicio.start(), fecha + 1);
            String corpo = fonte.substring(abre + 1, fecha);
            int linha = 1 + (int) fonte.substring(0, inicio.start()).chars().filter(c -> c == '\n').count();
            String onde = RAIZ_FONTES.relativize(arquivo) + ":" + linha;

            List<String> argumentos = separarArgumentos(corpo);
            String mensagem = argumentos.isEmpty() ? "" : unirLiterais(argumentos.get(0));

            if (getMessageComExcecaoNoFim(argumentos)) {
                violacoes.add(onde + " (a) getMessage() antes da exceção: " + resumo(chamada));
            }
            if (mensagem.contains("===")) {
                violacoes.add(onde + " (b) mensagem com '===': " + resumo(chamada));
            }
            verificarMascara(onde, chamada, mensagem, argumentos, violacoes);
        }
    }

    private void verificarMascara(String onde, String chamada, String mensagem, List<String> argumentos,
                                  List<String> violacoes) {
        int indicePlaceholder = 0;
        int posicao = 0;
        while (true) {
            int proximo = mensagem.indexOf("{}", posicao);
            if (proximo < 0) {
                return;
            }
            String trecho = mensagem.substring(Math.max(posicao, proximo - JANELA_CHAVE), proximo + 2);
            if (PLACEHOLDER_SENSIVEL.matcher(trecho).find()) {
                int indiceArgumento = indicePlaceholder + 1;
                String argumento = indiceArgumento < argumentos.size() ? argumentos.get(indiceArgumento) : "";
                if (!argumento.contains("MascaraLog.")) {
                    violacoes.add(onde + " (c) dado pessoal sem MascaraLog: " + resumo(chamada));
                }
            }
            indicePlaceholder++;
            posicao = proximo + 2;
        }
    }

    /** Verdadeiro quando o último argumento é uma variável de exceção típica e algum outro chama getMessage(). */
    private boolean getMessageComExcecaoNoFim(List<String> argumentos) {
        if (argumentos.size() < 2) {
            return false;
        }
        String ultimo = argumentos.get(argumentos.size() - 1);
        if (!VARIAVEL_DE_EXCECAO.matcher(ultimo).matches()) {
            return false;
        }
        return argumentos.subList(1, argumentos.size() - 1).stream().anyMatch(a -> a.contains("getMessage()"));
    }

    /** Troca comentários por espaços (mantendo as quebras de linha), respeitando literais de texto e char. */
    private String semComentarios(String fonte) {
        StringBuilder saida = new StringBuilder(fonte.length());
        for (int i = 0; i < fonte.length(); i++) {
            char c = fonte.charAt(i);
            if (c == '"' || c == '\'') {
                int fim = pularLiteral(fonte, i);
                saida.append(fonte, i, fim + 1);
                i = fim;
            } else if (c == '/' && i + 1 < fonte.length() && fonte.charAt(i + 1) == '/') {
                while (i < fonte.length() && fonte.charAt(i) != '\n') {
                    saida.append(' ');
                    i++;
                }
                i--;
            } else if (c == '/' && i + 1 < fonte.length() && fonte.charAt(i + 1) == '*') {
                int fim = fonte.indexOf("*/", i + 2);
                fim = fim < 0 ? fonte.length() - 1 : fim + 1;
                for (int j = i; j <= fim; j++) {
                    saida.append(fonte.charAt(j) == '\n' ? '\n' : ' ');
                }
                i = fim;
            } else {
                saida.append(c);
            }
        }
        return saida.toString();
    }

    /** Índice do parêntese que fecha o aberto em {@code abre}, ignorando aspas; -1 se não fechar. */
    private int encontrarFechamento(String fonte, int abre) {
        int profundidade = 0;
        for (int i = abre; i < fonte.length(); i++) {
            char c = fonte.charAt(i);
            if (c == '"' || c == '\'') {
                i = pularLiteral(fonte, i);
            } else if (c == '(') {
                profundidade++;
            } else if (c == ')') {
                profundidade--;
                if (profundidade == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private List<String> separarArgumentos(String corpo) {
        List<String> argumentos = new ArrayList<>();
        int profundidade = 0;
        int inicio = 0;
        for (int i = 0; i < corpo.length(); i++) {
            char c = corpo.charAt(i);
            if (c == '"' || c == '\'') {
                i = pularLiteral(corpo, i);
            } else if (c == '(' || c == '{' || c == '[') {
                profundidade++;
            } else if (c == ')' || c == '}' || c == ']') {
                profundidade--;
            } else if (c == ',' && profundidade == 0) {
                argumentos.add(corpo.substring(inicio, i).trim());
                inicio = i + 1;
            }
        }
        argumentos.add(corpo.substring(inicio).trim());
        return argumentos;
    }

    /** Devolve o índice do último caractere do literal (de texto ou char) que começa em {@code i}. */
    private int pularLiteral(String texto, int i) {
        char aspas = texto.charAt(i);
        for (int j = i + 1; j < texto.length(); j++) {
            char c = texto.charAt(j);
            if (c == '\\') {
                j++;
            } else if (c == aspas) {
                return j;
            }
        }
        return texto.length() - 1;
    }

    /** Junta o conteúdo de todos os literais de texto do argumento ({@code "a" + "b"} vira {@code ab}). */
    private String unirLiterais(String argumento) {
        StringBuilder unido = new StringBuilder();
        for (int i = 0; i < argumento.length(); i++) {
            if (argumento.charAt(i) == '"') {
                int fim = pularLiteral(argumento, i);
                unido.append(argumento, i + 1, Math.max(i + 1, fim));
                i = fim;
            }
        }
        return unido.toString();
    }

    private String resumo(String chamada) {
        return chamada.replaceAll("\\s+", " ");
    }
}
