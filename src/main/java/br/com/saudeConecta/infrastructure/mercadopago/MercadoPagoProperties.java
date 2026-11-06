package br.com.saudeConecta.infrastructure.mercadopago;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Credenciais e endpoints da integração com o Mercado Pago.
 * Todos os valores sensíveis são lidos de variáveis de ambiente —
 * nenhum segredo deve ser versionado nos arquivos de propriedades.
 */
@Component
@ConfigurationProperties(prefix = "mercadopago")
@Getter
@Setter
public class MercadoPagoProperties {

    /** Access Token da aplicação (env MP_ACCESS_TOKEN). */
    private String accessToken;

    /** Public Key da aplicação (env MP_PUBLIC_KEY). */
    private String publicKey;

    /** Secret usado para validar a assinatura HMAC dos webhooks (env MP_WEBHOOK_SECRET). */
    private String webhookSecret;

    /** URL de retorno do checkout hospedado (env MP_BACK_URL). */
    private String backUrl;

    /** Base da API do Mercado Pago. Sobrescrita apenas em testes. */
    private String apiUrl = "https://api.mercadopago.com";

    /** Prefixo do campo reason exibido na fatura do cliente. */
    private String reasonPrefix = "Saúde Conecta";

    /**
     * Indica se a integração está configurada o suficiente para operar.
     *
     * @return true quando access token e back url estão presentes
     */
    public boolean isConfigurado() {
        return accessToken != null && !accessToken.isBlank()
                && backUrl != null && !backUrl.isBlank();
    }
}
