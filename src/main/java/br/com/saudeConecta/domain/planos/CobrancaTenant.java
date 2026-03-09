package br.com.saudeConecta.domain.planos;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobranca_tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class CobrancaTenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_tenant_id", nullable = false)
    private AssinaturaTenant assinaturaTenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCobranca status = StatusCobranca.PENDENTE;

    @Column(name = "pix_copia_cola", columnDefinition = "TEXT")
    private String pixCopiaECola;

    @Column(name = "pix_qrcode_base64", columnDefinition = "LONGTEXT")
    private String pixQrcodeBase64;

    @Column(length = 100, unique = true)
    private String txid;

    @Column(name = "data_vencimento_pix")
    private LocalDate dataVencimentoPix;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Column(name = "criada_em", updatable = false)
    private LocalDateTime criadaEm;

    @PrePersist
    protected void onCreate() {
        criadaEm = LocalDateTime.now();
    }

    /**
     * Verifica se a cobrança já foi paga.
     *
     * @return true se status == PAGO
     */
    public boolean isPago() {
        return StatusCobranca.PAGO.equals(this.status);
    }

    /**
     * Verifica se o Pix da cobrança expirou.
     *
     * @return true se a data de vencimento do Pix já passou
     */
    public boolean isPixExpirado() {
        return dataVencimentoPix != null && LocalDate.now().isAfter(dataVencimentoPix);
    }
}
