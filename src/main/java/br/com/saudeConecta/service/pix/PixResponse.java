package br.com.saudeConecta.service.pix;

import java.time.LocalDate;

public record PixResponse(
        String txid,
        String pixCopiaECola,
        String qrcodeBase64,
        LocalDate dataVencimento
) {
}
