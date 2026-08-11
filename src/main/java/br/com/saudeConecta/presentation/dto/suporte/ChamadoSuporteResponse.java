package br.com.saudeConecta.presentation.dto.suporte;

import br.com.saudeConecta.domain.suporte.CategoriaChamado;
import br.com.saudeConecta.domain.suporte.ChamadoSuporte;
import br.com.saudeConecta.domain.suporte.PrioridadeChamado;
import br.com.saudeConecta.domain.suporte.StatusChamado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * DTO de saida para chamados de suporte.
 *
 * @param id                  identificador aleatorio do chamado (ex.: 230423-324234-32423)
 * @param protocolo           protocolo legivel (ex.: #SC-230423-324234-32423)
 * @param organizacaoId       ID da organizacao dona do chamado
 * @param usuarioCriadorId    ID do usuario que abriu o chamado
 * @param criadorNome         nome do usuario que abriu o chamado
 * @param criadorEmail        email do usuario que abriu o chamado
 * @param titulo              titulo do chamado
 * @param corpo               descricao detalhada
 * @param categoria           categoria do chamado
 * @param categoriaDescricao  descricao amigavel da categoria
 * @param prioridade          prioridade do chamado
 * @param prioridadeDescricao descricao amigavel da prioridade
 * @param status              status atual
 * @param statusDescricao     descricao amigavel do status
 * @param previsaoAtendimento data prevista de atendimento
 * @param dataCriacao         data de abertura
 * @param dataAtualizacao     data da ultima atualizacao
 * @param anexos              lista de anexos do chamado
 */
public record ChamadoSuporteResponse(
        String id,
        String protocolo,
        Long organizacaoId,
        Long usuarioCriadorId,
        String criadorNome,
        String criadorEmail,
        String titulo,
        String corpo,
        CategoriaChamado categoria,
        String categoriaDescricao,
        PrioridadeChamado prioridade,
        String prioridadeDescricao,
        StatusChamado status,
        String statusDescricao,
        LocalDate previsaoAtendimento,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        List<ChamadoAnexoResponse> anexos
) {
    /**
     * Converte a entidade para DTO sem carregar a colecao de anexos.
     * Indicado para listagens, evitando trafegar imagens em base64.
     *
     * @param chamado entidade a ser convertida
     * @return DTO de resposta sem anexos
     */
    public static ChamadoSuporteResponse fromEntity(ChamadoSuporte chamado) {
        return montar(chamado, Collections.emptyList());
    }

    /**
     * Converte a entidade para DTO incluindo os anexos.
     *
     * @param chamado entidade a ser convertida (com anexos carregados)
     * @return DTO de resposta com anexos
     */
    public static ChamadoSuporteResponse fromEntityComAnexos(ChamadoSuporte chamado) {
        List<ChamadoAnexoResponse> anexos = chamado.getAnexos() == null
                ? Collections.emptyList()
                : chamado.getAnexos().stream().map(ChamadoAnexoResponse::fromEntity).toList();
        return montar(chamado, anexos);
    }

    private static ChamadoSuporteResponse montar(ChamadoSuporte chamado, List<ChamadoAnexoResponse> anexos) {
        return new ChamadoSuporteResponse(
                chamado.getId(),
                chamado.getProtocolo(),
                chamado.getOrganizacaoId(),
                chamado.getUsuarioCriador() != null ? chamado.getUsuarioCriador().getId() : null,
                chamado.getCriadorNome(),
                chamado.getCriadorEmail(),
                chamado.getTitulo(),
                chamado.getCorpo(),
                chamado.getCategoria(),
                chamado.getCategoria() != null ? chamado.getCategoria().getDescricao() : null,
                chamado.getPrioridade(),
                chamado.getPrioridade() != null ? chamado.getPrioridade().getDescricao() : null,
                chamado.getStatus(),
                chamado.getStatus() != null ? chamado.getStatus().getDescricao() : null,
                chamado.getPrevisaoAtendimento(),
                chamado.getDataCriacao(),
                chamado.getDataAtualizacao(),
                anexos
        );
    }
}
