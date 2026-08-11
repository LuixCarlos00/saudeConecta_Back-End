package br.com.saudeConecta.presentation.dto.suporte;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para um anexo de imagem do chamado de suporte.
 *
 * @param nomeArquivo    nome original do arquivo
 * @param tipoConteudo   mime type do arquivo (ex.: image/png)
 * @param conteudoBase64 conteudo do arquivo codificado em base64 (aceita data URL)
 */
public record ChamadoAnexoRequest(

        @NotBlank(message = "O nome do arquivo e obrigatorio")
        String nomeArquivo,

        String tipoConteudo,

        @NotBlank(message = "O conteudo do arquivo e obrigatorio")
        String conteudoBase64
) {
}
