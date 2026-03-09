package br.com.saudeConecta.service.pix;

import br.com.saudeConecta.infra.exceptions.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação real do PixService usando API Asaas.
 * Ativa quando asaas.enabled=true no application.properties.
 *
 * Fluxo:
 * 1. Criar cobrança PIX: POST /v3/payments (billingType=PIX)
 * 2. Obter QR Code: GET /v3/payments/{id}/pixQrCode
 * 3. Webhook: Asaas envia PAYMENT_RECEIVED com payment.id
 */
// @Service — desativado: usando Pix Estático (PixServiceEstatico) em vez de PSP Asaas
// @ConditionalOnProperty(name = "asaas.enabled", havingValue = "true")
public class PixServiceAsaas /* implements PixService — desativado */ {

    private static final Logger log = LoggerFactory.getLogger(PixServiceAsaas.class);
    private static final int DIAS_VENCIMENTO_PIX = 3;

    @Value("${asaas.api.url}")
    private String apiUrl;

    @Value("${asaas.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PixServiceAsaas() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gera uma cobrança Pix no Asaas e retorna os dados do QR Code.
     *
     * @param valor           valor da cobrança
     * @param descricao       descrição da cobrança
     * @param asaasCustomerId ID do cliente no Asaas
     * @return PixResponse com dados do Pix
     */
    public PixResponse gerarCobrancaPix(BigDecimal valor, String descricao, String asaasCustomerId) {
        try {
            // 1. Criar cobrança PIX no Asaas
            String paymentId = criarCobrancaAsaas(asaasCustomerId, valor, descricao);

            // 2. Obter QR Code e copia-e-cola
            return obterDadosPix(paymentId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao gerar cobrança Pix no Asaas: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Erro ao gerar cobrança Pix: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Consulta o status de um pagamento no Asaas.
     *
     * @param paymentId ID do pagamento no Asaas (ex: pay_xxxxx)
     * @return true se o pagamento foi confirmado (RECEIVED ou CONFIRMED)
     */
    public boolean consultarPagamento(String paymentId) {
        try {
            HttpHeaders headers = criarHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/v3/payments/" + paymentId,
                    HttpMethod.GET,
                    entity,
                    String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String status = json.get("status").asText();

            log.info("Consulta pagamento Asaas: paymentId={}, status={}", paymentId, status);

            return "RECEIVED".equals(status) || "CONFIRMED".equals(status);

        } catch (Exception e) {
            log.error("Erro ao consultar pagamento no Asaas: paymentId={}, erro={}", paymentId, e.getMessage());
            return false;
        }
    }

    /**
     * Cria um cliente no Asaas.
     * Necessário para gerar cobranças.
     *
     * @param nome    nome do cliente (organização)
     * @param cpfCnpj CPF ou CNPJ
     * @param email   email (opcional)
     * @return ID do cliente criado (ex: cus_xxxxx)
     */
    public String criarCliente(String nome, String cpfCnpj, String email) {
        try {
            HttpHeaders headers = criarHeaders();

            Map<String, Object> body = new HashMap<>();
            body.put("name", nome);
            body.put("cpfCnpj", limparCpfCnpj(cpfCnpj));
            if (email != null && !email.isBlank()) {
                body.put("email", email);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/v3/customers",
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String customerId = json.get("id").asText();

            log.info("Cliente criado no Asaas: id={}, nome={}", customerId, nome);
            return customerId;

        } catch (Exception e) {
            log.error("Erro ao criar cliente no Asaas: nome={}, erro={}", nome, e.getMessage(), e);
            throw new BusinessException(
                    "Erro ao criar cliente no Asaas: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Cria uma cobrança PIX no Asaas.
     *
     * @param customerId ID do cliente no Asaas
     * @param valor      valor da cobrança
     * @param descricao  descrição
     * @return ID do pagamento criado (ex: pay_xxxxx)
     */
    private String criarCobrancaAsaas(String customerId, BigDecimal valor, String descricao) {
        HttpHeaders headers = criarHeaders();

        LocalDate vencimento = LocalDate.now().plusDays(DIAS_VENCIMENTO_PIX);

        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", valor.doubleValue());
        body.put("dueDate", vencimento.format(DateTimeFormatter.ISO_LOCAL_DATE));
        body.put("description", descricao);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/v3/payments",
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String paymentId = json.get("id").asText();

            log.info("Cobrança PIX criada no Asaas: paymentId={}, valor=R${}, vencimento={}",
                    paymentId, valor, vencimento);

            return paymentId;

        } catch (Exception e) {
            log.error("Erro ao criar cobrança no Asaas: {}", e.getMessage(), e);
            throw new BusinessException(
                    "Erro ao criar cobrança no Asaas: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtém dados do QR Code Pix de um pagamento no Asaas.
     *
     * @param paymentId ID do pagamento (ex: pay_xxxxx)
     * @return PixResponse com QR Code, copia-e-cola e vencimento
     */
    private PixResponse obterDadosPix(String paymentId) {
        HttpHeaders headers = criarHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/v3/payments/" + paymentId + "/pixQrCode",
                    HttpMethod.GET,
                    entity,
                    String.class);

            JsonNode json = objectMapper.readTree(response.getBody());

            String encodedImage = json.get("encodedImage").asText();
            String payload = json.get("payload").asText();
            String expirationDate = json.get("expirationDate").asText();

            LocalDate vencimento = LocalDate.parse(
                    expirationDate.substring(0, 10),
                    DateTimeFormatter.ISO_LOCAL_DATE);

            log.info("QR Code Pix obtido do Asaas: paymentId={}, vencimento={}", paymentId, vencimento);

            return new PixResponse(
                    paymentId,
                    payload,
                    encodedImage,
                    vencimento
            );

        } catch (Exception e) {
            log.error("Erro ao obter QR Code do Asaas: paymentId={}, erro={}", paymentId, e.getMessage(), e);
            throw new BusinessException(
                    "Erro ao obter QR Code Pix: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Cria headers HTTP com autenticação Asaas.
     *
     * @return HttpHeaders configurados
     */
    private HttpHeaders criarHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", apiKey);
        return headers;
    }

    /**
     * Remove caracteres não numéricos do CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ com ou sem formatação
     * @return apenas os dígitos
     */
    private String limparCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null) return null;
        return cpfCnpj.replaceAll("[^0-9]", "");
    }
}
