package br.com.saudeConecta.domain.planos;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
// Uma fatura do Mercado Pago gera no máximo uma cobrança local (changeset 064)
@Table(name = "cobranca_tenant", uniqueConstraints = @UniqueConstraint(
        name = "uk_cobranca_mp_authorized_payment", columnNames = "mp_authorized_payment_id"))
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

    /**
     * Identificador da transação Pix. Mantido apenas para leitura do histórico
     * das cobranças anteriores à migração para o Mercado Pago.
     */
    @Column(length = 100, unique = true)
    private String txid;

    @Column(name = "mp_payment_id", length = 50)
    private String mpPaymentId;

    @Column(name = "mp_authorized_payment_id", length = 50)
    private String mpAuthorizedPaymentId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamentoCobranca formaPagamento = FormaPagamentoCobranca.MERCADO_PAGO;

    @Column(name = "motivo_recusa", length = 255)
    private String motivoRecusa;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Column(name = "criada_em", updatable = false)
    private LocalDateTime criadaEm;

    @PrePersist
    protected void onCreate() {
        criadaEm = LocalDateTime.now(FusoHorario.ZONA);
    }

    /**
     * Verifica se a cobrança já foi paga.
     *
     * @return true se status == PAGO
     */
    public boolean isPago() {
        return StatusCobranca.PAGO.equals(this.status);
    }
}
