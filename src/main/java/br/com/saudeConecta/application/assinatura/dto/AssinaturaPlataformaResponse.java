package br.com.saudeConecta.application.assinatura.dto;

import br.com.saudeConecta.domain.planos.AssinaturaTenant;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha da tela de assinaturas do SUPER_ADMIN: a assinatura vigente de uma clínica. Sem dado
 * pessoal (nem e-mail do pagador, nem link de checkout, nem id do preapproval): só se há preapproval.
 *
 * @param assinaturaId                       id da assinatura (usado em cortesia/suspender/cancelar)
 * @param organizacaoId                      id da clínica
 * @param organizacaoNome                    nome da clínica
 * @param planoId                            id do plano base
 * @param planoNome                          nome do plano base
 * @param planoTipo                          STARTER, PROFISSIONAL ou BUSINESS
 * @param status                             TRIAL, ATIVA, INADIMPLENTE, SUSPENSA ou CANCELADA
 * @param valorMensal                        valor cobrado por mês (plano + extras)
 * @param cortesia                           true = isenta de cobrança
 * @param dataVencimento                     vencimento atual
 * @param mpStatus                           status do preapproval no Mercado Pago (null sem preapproval)
 * @param temPreapproval                     true se há preapproval vinculado
 * @param preapprovalSubstituidoPendente     true se um preapproval antigo ainda aguarda cancelamento no MP
 */
public record AssinaturaPlataformaResponse(
        Long assinaturaId,
        Long organizacaoId,
        String organizacaoNome,
        Long planoId,
        String planoNome,
        String planoTipo,
        String status,
        BigDecimal valorMensal,
        boolean cortesia,
        LocalDate dataVencimento,
        String mpStatus,
        boolean temPreapproval,
        boolean preapprovalSubstituidoPendente
) {

    /**
     * @param a assinatura com organização e plano carregados
     * @return a linha da listagem
     */
    public static AssinaturaPlataformaResponse fromEntity(AssinaturaTenant a) {
        return new AssinaturaPlataformaResponse(
                a.getId(),
                a.getOrganizacao().getId(),
                a.getOrganizacao().getNome(),
                a.getPlanoAssinatura().getId(),
                a.getPlanoAssinatura().getNome(),
                a.getPlanoAssinatura().getTipo().name(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getValorMensal(),
                a.isCortesia(),
                a.getDataVencimento(),
                a.getMpStatus(),
                a.temPreapproval(),
                a.temPreapprovalSubstituidoPendente());
    }
}
