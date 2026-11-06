package br.com.saudeConecta.application.plataforma.dto;

import java.math.BigDecimal;

/**
 * @param mes             mês no formato yyyy-MM
 * @param novasClinicas   assinaturas criadas no mês
 * @param cancelamentos   assinaturas canceladas no mês
 * @param receitaRecebida cobranças PAGO com pagamento no mês
 * @param consultas       consultas do mês, exceto canceladas
 */
public record SerieMensalResponse(String mes, long novasClinicas, long cancelamentos,
                                  BigDecimal receitaRecebida, long consultas) {
}
