package br.com.saudeConecta.presentation.dto.mensageria;

import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.mensageria.StatusMensagem;
import br.com.saudeConecta.domain.mensageria.TipoMensagem;

import java.time.LocalDateTime;

/**
 * DTO de resposta para registros de mensageria.
 *
 * @param id                        Identificador único do registro
 * @param organizacaoId             ID da organização
 * @param destinatarioProfissionalId ID do profissional destinatário (pode ser nulo)
 * @param destinatarioProfissionalNome Nome do profissional destinatário
 * @param destinatarioEmail         Email do destinatário
 * @param destinatarioNome          Nome do destinatário
 * @param assunto                   Assunto da mensagem
 * @param corpoMensagem             Corpo completo da mensagem
 * @param tipoMensagem              Tipo da mensagem
 * @param status                    Status atual da mensagem
 * @param erroDetalhe               Detalhe do erro em caso de falha
 * @param tentativas                Número de tentativas de envio
 * @param adminNotificado           Se o admin foi notificado sobre a falha
 * @param dataCriacao               Data de criação do registro
 * @param dataAtualizacao           Data da última atualização
 */
public record MensageriaResponse(
        Long id,
        Long organizacaoId,
        Long destinatarioProfissionalId,
        String destinatarioProfissionalNome,
        String destinatarioEmail,
        String destinatarioNome,
        String assunto,
        String corpoMensagem,
        TipoMensagem tipoMensagem,
        StatusMensagem status,
        String erroDetalhe,
        Integer tentativas,
        Boolean adminNotificado,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
    /**
     * Converte entidade Mensageria para DTO de resposta.
     *
     * @param mensageria Entidade a ser convertida
     * @return DTO de resposta
     */
    public static MensageriaResponse fromEntity(Mensageria mensageria) {
        return new MensageriaResponse(
                mensageria.getId(),
                mensageria.getOrganizacaoId(),
                mensageria.getDestinatarioProfissional() != null ? mensageria.getDestinatarioProfissional().getId() : null,
                mensageria.getDestinatarioProfissional() != null ? mensageria.getDestinatarioProfissional().getNome() : null,
                mensageria.getDestinatarioEmail(),
                mensageria.getDestinatarioNome(),
                mensageria.getAssunto(),
                mensageria.getCorpoMensagem(),
                mensageria.getTipoMensagem(),
                mensageria.getStatus(),
                mensageria.getErroDetalhe(),
                mensageria.getTentativas(),
                mensageria.getAdminNotificado(),
                mensageria.getDataCriacao(),
                mensageria.getDataAtualizacao()
        );
    }
}
