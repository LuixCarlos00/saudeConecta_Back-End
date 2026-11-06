package br.com.saudeConecta.presentation.erro;

import br.com.saudeConecta.domain.FusoHorario;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de erro da API (produzido pelo {@link GlobalExceptionHandler} e pelo FiltroAcesso).
 *
 * <p>Contrato com o front:
 * <ul>
 *   <li>{@code message}: texto para o usuário. É o primeiro campo lido pelo {@code extrairMensagemErro}
 *       do front e é por ele que o ErrorInterceptor detecta plano suspenso no 403.</li>
 *   <li>{@code error}: alias de {@code message}. Mantido porque a tela de login (401) e telas antigas
 *       leem {@code {error}} como texto; por isso NÃO é o reason phrase do HTTP.</li>
 *   <li>{@code code}: código estável para máquina (ex.: CONFLITO, VALIDACAO).</li>
 *   <li>{@code errors}: só em erro de validação, mapa campo → mensagem.</li>
 * </ul>
 *
 * @param timestamp momento do erro
 * @param status    status HTTP numérico
 * @param error     alias de {@code message} (compatibilidade com o front)
 * @param code      código do erro para máquina
 * @param message   mensagem legível, sem detalhes internos
 * @param path      URI da requisição (null fora de uma requisição)
 * @param errors    erros por campo (apenas validação)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> errors
) {

    /**
     * Cria a resposta padrão.
     *
     * @param status  status HTTP numérico
     * @param code    código do erro
     * @param message mensagem para o usuário
     * @param path    URI da requisição
     * @return resposta de erro
     */
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(FusoHorario.ZONA), status, message, code, message, path, null);
    }

    /**
     * Cria a resposta de erro de validação com o mapa campo → mensagem.
     *
     * @param status  status HTTP numérico
     * @param code    código do erro
     * @param message mensagem para o usuário (as mensagens dos campos juntas)
     * @param path    URI da requisição
     * @param errors  mapa campo → mensagem
     * @return resposta de erro
     */
    public static ErrorResponse ofValidation(int status, String code, String message, String path,
                                             Map<String, String> errors) {
        return new ErrorResponse(LocalDateTime.now(FusoHorario.ZONA), status, message, code, message, path, errors);
    }
}
