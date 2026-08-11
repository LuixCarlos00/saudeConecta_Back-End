package br.com.saudeConecta.presentation.dto.suporte;

import br.com.saudeConecta.domain.suporte.ChamadoAnexo;

import java.time.LocalDateTime;

/**
 * DTO de saida para um anexo de chamado de suporte.
 *
 * @param id             identificador do anexo
 * @param nomeArquivo    nome original do arquivo
 * @param tipoConteudo   mime type do arquivo
 * @param tamanhoBytes   tamanho aproximado do arquivo em bytes
 * @param conteudoBase64 conteudo do arquivo em base64
 * @param dataCriacao    data de criacao do anexo
 */
public record ChamadoAnexoResponse(
        Long id,
        String nomeArquivo,
        String tipoConteudo,
        Long tamanhoBytes,
        String conteudoBase64,
        LocalDateTime dataCriacao
) {
    /**
     * Converte a entidade de anexo para DTO de resposta.
     *
     * @param anexo entidade a ser convertida
     * @return DTO de resposta
     */
    public static ChamadoAnexoResponse fromEntity(ChamadoAnexo anexo) {
        return new ChamadoAnexoResponse(
                anexo.getId(),
                anexo.getNomeArquivo(),
                anexo.getTipoConteudo(),
                anexo.getTamanhoBytes(),
                anexo.getConteudoBase64(),
                anexo.getDataCriacao()
        );
    }
}
