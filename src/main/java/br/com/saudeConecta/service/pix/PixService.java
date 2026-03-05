package br.com.saudeConecta.service.pix;

import java.math.BigDecimal;

public interface PixService {

    /**
     * Gera uma cobrança Pix dinâmica junto ao PSP.
     *
     * @param valor     valor da cobrança
     * @param descricao descrição da cobrança
     * @return PixResponse com txid, copia-e-cola, QR Code e vencimento
     */
    PixResponse gerarCobrancaPix(BigDecimal valor, String descricao);

    /**
     * Consulta o status de um pagamento Pix pelo txid.
     *
     * @param txid identificador da transação
     * @return true se o pagamento foi confirmado
     */
    boolean consultarPagamento(String txid);
}
