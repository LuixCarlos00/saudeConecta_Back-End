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

    @Column(name = "limite_admin_org_custom")
    private Integer limiteAdminOrgCustom;

    @Column(name = "limite_profissional_custom")
    private Integer limiteProfissionalCustom;

    @Column(name = "limite_secretaria_custom")
    private Integer limiteSecretariaCustom;

    @Column(name = "qtd_admin_extra", nullable = false)
    private Integer qtdAdminExtra = 0;

    @Column(name = "qtd_profissional_extra", nullable = false)
    private Integer qtdProfissionalExtra = 0;

    @Column(name = "qtd_secretaria_extra", nullable = false)
    private Integer qtdSecretariaExtra = 0;

    @Column(name = "valor_adicional_perfis", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAdicionalPerfis = BigDecimal.ZERO;

    @Column(name = "mp_preapproval_id", length = 50, unique = true)
    private String mpPreapprovalId;

    @Column(name = "mp_status", length = 30)
    private String mpStatus;

    @Column(name = "mp_init_point", length = 500)
    private String mpInitPoint;

    @Column(name = "mp_payer_email", length = 150)
    private String mpPayerEmail;

    @Column(name = "mp_ultima_sincronizacao")
    private LocalDateTime mpUltimaSincronizacao;

    @Builder.Default
    @Column(name = "cortesia", nullable = false)
    private Boolean cortesia = false;

    @Column(name = "data_cancelamento")
    private LocalDateTime dataCancelamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelado_por", length = 20)
    private OrigemCancelamento canceladoPor;

    /** Momento em que a assinatura entrou em SUSPENSA; base do prazo para cancelamento. */
    @Column(name = "data_suspensao")
    private LocalDateTime dataSuspensao;

    /** Dia do mês do ciclo de cobrança, fixado no primeiro pagamento (1 a 31). */
    @Column(name = "dia_cobranca")
    private Integer diaCobranca;

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

    /**
     * Indica se a assinatura é uma cortesia (liberada manualmente pelo SUPER_ADMIN).
     * Assinaturas de cortesia são ignoradas por toda a rotina de cobrança.
     *
     * @return true se a assinatura não deve ser cobrada
     */
    public boolean isCortesia() {
        return Boolean.TRUE.equals(cortesia);
    }

    /**
     * Indica se a assinatura já possui um preapproval criado no Mercado Pago.
     *
     * @return true se existe preapproval vinculado
     */
    public boolean temPreapproval() {
        return mpPreapprovalId != null && !mpPreapprovalId.isBlank();
    }

    /**
     * Avança o vencimento para o próximo ciclo mensal após uma fatura paga.
     *
     * O dia âncora é fixado no primeiro pagamento e preservado nos meses curtos
     * (31/jan → 28/fev → 31/mar). O próximo vencimento é calculado a partir do
     * maior valor entre o vencimento atual e a data do pagamento: assim um pagamento
     * antecipado não gera vencimento no dia seguinte e um atrasado não acumula ciclos.
     *
     * @param dataPagamento data em que a fatura foi paga
     */
    public void renovarCiclo(LocalDate dataPagamento) {
        if (diaCobranca == null) {
            diaCobranca = dataPagamento.getDayOfMonth();
        }
        LocalDate base = dataVencimento != null && dataVencimento.isAfter(dataPagamento)
                ? dataVencimento
                : dataPagamento;

        LocalDate proximo = noDiaDeCobranca(base);
        if (!proximo.isAfter(base)) {
            proximo = noDiaDeCobranca(base.plusMonths(1));
        }
        dataVencimento = proximo;
        dataProximaCobranca = proximo;
    }

    private LocalDate noDiaDeCobranca(LocalDate mes) {
        return mes.withDayOfMonth(Math.min(diaCobranca, mes.lengthOfMonth()));
    }
}
