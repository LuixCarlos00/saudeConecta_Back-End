package br.com.saudeConecta.application.notificacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados do email com o link de checkout do Mercado Pago enviado antes do fim do trial.
 *
 * @param email         email do administrador responsável
 * @param nome          nome do administrador
 * @param plano         nome do plano contratado
 * @param valorMensal   valor que será cobrado mensalmente
 * @param initPoint     URL do checkout do Mercado Pago
 * @param payerEmail    e-mail que deve estar vinculado à conta Mercado Pago do pagador
 * @param dataFimTrial  data em que o período gratuito termina
 * @param organizacaoId ID da organização
 * @param adminOrgId    ID do admin da organização
 */
public record DadosEmailAtivacaoAssinatura(String email, String nome, String plano,
                                           BigDecimal valorMensal, String initPoint, String payerEmail,
                                           LocalDate dataFimTrial, Long organizacaoId, Long adminOrgId) {
}
