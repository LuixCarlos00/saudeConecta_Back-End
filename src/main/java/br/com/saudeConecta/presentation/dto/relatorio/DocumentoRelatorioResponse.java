package br.com.saudeConecta.presentation.dto.relatorio;

import br.com.saudeConecta.domain.relatorio.TipoDocumentoRelatorio;

import java.time.LocalDateTime;

/**
 * Documento disponivel para visualizacao ou impressao.
 *
 * @param id           identificador da origem do documento (prontuario, planejamento, termo ou consulta)
 * @param tipo         tipo do documento
 * @param titulo       titulo exibido no card
 * @param descricao    resumo do conteudo do documento
 * @param emitidoEm    data e hora em que o documento foi gerado
 * @param assinado     indica se possui assinatura do paciente
 * @param consultaId   consulta que originou o documento
 */
public record DocumentoRelatorioResponse(
        Long id,
        TipoDocumentoRelatorio tipo,
        String titulo,
        String descricao,
        LocalDateTime emitidoEm,
        boolean assinado,
        Long consultaId
) {
}
