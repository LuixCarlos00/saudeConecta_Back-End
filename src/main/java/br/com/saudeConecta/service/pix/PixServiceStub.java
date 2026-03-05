package br.com.saudeConecta.service.pix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

/**
 * Implementação stub do PixService para desenvolvimento e testes.
 * Substitua por integração real com PSP (EfiPay, Mercado Pago, etc.)
 * alterando o @Service para a implementação concreta.
 */
@Service
public class PixServiceStub implements PixService {

    private static final Logger log = LoggerFactory.getLogger(PixServiceStub.class);
    private static final int DIAS_VENCIMENTO_PIX = 3;

    @Override
    public PixResponse gerarCobrancaPix(BigDecimal valor, String descricao) {
        String txid = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String pixCopiaECola = "00020126580014br.gov.bcb.pix0136" + txid + "5204000053039865802BR5925SAUDE CONECTA LTDA6009SAO PAULO62070503***6304";
        String qrcodeBase64 = Base64.getEncoder().encodeToString(
                ("PIX_STUB_QRCODE_" + txid + "_VALOR_" + valor).getBytes());
        LocalDate vencimento = LocalDate.now().plusDays(DIAS_VENCIMENTO_PIX);

        log.info("[STUB] Cobrança Pix gerada: txid={}, valor=R${}, vencimento={}",
                txid, valor, vencimento);

        return new PixResponse(txid, pixCopiaECola, qrcodeBase64, vencimento);
    }

    @Override
    public boolean consultarPagamento(String txid) {
        log.info("[STUB] Consulta de pagamento Pix: txid={} — retornando false (stub)", txid);
        return false;
    }
}
