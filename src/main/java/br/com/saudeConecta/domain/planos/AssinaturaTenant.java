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
@Table(name = "assinatura_tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class AssinaturaTenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_assinatura_id", nullable = false)
    private PlanoAssinatura planoAssinatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAssinatura status = StatusAssinatura.TRIAL;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_proxima_cobranca")
    private LocalDate dataProximaCobranca;

    @Column(name = "valor_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensal;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    /**
     * Verifica se a assinatura permite acesso ao sistema.
     * TRIAL, ATIVA e INADIMPLENTE permitem acesso.
     * SUSPENSA e CANCELADA bloqueiam.
     *
     * @return true se o tenant pode acessar o sistema
     */
    public boolean permiteAcesso() {
        return status != null && status.permiteAcesso();
    }

    /**
     * Verifica se a assinatura está vencida.
     *
     * @return true se a data de vencimento já passou
     */
    public boolean isVencida() {
        return dataVencimento != null && LocalDate.now().isAfter(dataVencimento);
    }
}
