package br.com.saudeConecta.service.pix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Implementação de Pix Estático usando chave Pix do recebedor.
 * Gera payload no padrão EMV/BRCode e QR Code via ZXing.
 * Confirmação de pagamento é manual (SuperAdmin).
 */
@Service
public class PixServiceEstatico   {

    private static final Logger log = LoggerFactory.getLogger(PixServiceEstatico.class);
    private static final int DIAS_VENCIMENTO_PIX = 10;
    private static final int QR_CODE_SIZE = 300;

    @Value("${pix.chave}")
    private String pixChave;

    @Value("${pix.nome}")
    private String pixNome;

    @Value("${pix.cidade}")
    private String pixCidade;


    public  PixResponse gerarCobrancaPix(BigDecimal valor  ) {
        String txid = gerarTxid();
        String payload = montarPayloadPixEstatico(valor, txid);
        String qrcodeBase64 = gerarQrCodeBase64(payload);
        LocalDate vencimento = LocalDate.now().plusDays(DIAS_VENCIMENTO_PIX);

        log.info("Cobrança Pix gerada: txid={}, valor=R${}, vencimento={}", txid, valor, vencimento);

        return new PixResponse(txid, payload, qrcodeBase64, vencimento);
    }

    /**
     * Monta o payload Pix no padrão EMV/BRCode (BR Code).
     * Referência: https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/II-ManualdePadroesparaIniciacaodoPix.pdf
     *
     * @param valor valor da cobrança
     * @param txid  identificador da transação
     * @return payload Pix formatado com CRC16
     */
    private String montarPayloadPixEstatico(BigDecimal valor, String txid) {
        StringBuilder payload = new StringBuilder();

        // Payload Format Indicator
        payload.append(campo("00", "01"));

        // Merchant Account Information (chave Pix)
        String gui = campo("00", "br.gov.bcb.pix");
        String chave = campo("01", pixChave);
        payload.append(campo("26", gui + chave));

        // Merchant Category Code
        payload.append(campo("52", "0000"));

        // Transaction Currency (986 = BRL)
        payload.append(campo("53", "986"));

        // Transaction Amount
        if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0) {
            payload.append(campo("54", valor.toPlainString()));
        }

        // Country Code
        payload.append(campo("58", "BR"));

        // Merchant Name (max 25 chars)
        String nome = pixNome.length() > 25 ? pixNome.substring(0, 25) : pixNome;
        payload.append(campo("59", nome));

        // Merchant City (max 15 chars)
        String cidade = pixCidade.length() > 15 ? pixCidade.substring(0, 15) : pixCidade;
        payload.append(campo("60", cidade));

        // Additional Data Field (txid)
        String txidField = campo("05", txid);
        payload.append(campo("62", txidField));

        // CRC16 — o campo "6304" faz parte do cálculo
        payload.append("6304");
        String crc = calcularCRC16(payload.toString());

        return payload.toString() + crc;
    }

    /**
     * Monta um campo TLV (Tag-Length-Value) do padrão EMV.
     *
     * @param id    identificador do campo (2 dígitos)
     * @param valor conteúdo do campo
     * @return campo formatado
     */
    private String campo(String id, String valor) {
        int tamanho = valor.getBytes(StandardCharsets.UTF_8).length;
        return id + String.format("%02d", tamanho) + valor;
    }

    /**
     * Calcula CRC16-CCITT-FALSE para o payload Pix.
     *
     * @param payload payload completo (incluindo "6304")
     * @return CRC16 em hexadecimal (4 chars)
     */
    private String calcularCRC16(String payload) {
        int crc = 0xFFFF;
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }

        return String.format("%04X", crc);
    }

    /**
     * Gera QR Code em base64 a partir do payload Pix.
     *
     * @param payload payload Pix completo
     * @return imagem QR Code codificada em base64
     */
    private String gerarQrCodeBase64(String payload) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );

            BitMatrix bitMatrix = qrCodeWriter.encode(payload, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (WriterException | IOException e) {
            log.error("Erro ao gerar QR Code: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Gera um txid único para identificar a transação.
     *
     * @return txid alfanumérico de 25 caracteres
     */
    private String gerarTxid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 25);
    }
}
