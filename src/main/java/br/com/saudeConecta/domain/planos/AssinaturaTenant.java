package br.com.saudeConecta.domain.planos;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.infrastructure.persistence.listener.AssinaturaTenantCacheListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assinatura_tenant")
@EntityListeners(AssinaturaTenantCacheListener.class)
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

    /**
     * Preapproval anterior que foi trocado por um novo e ainda precisa ser cancelado no
     * Mercado Pago. O cancelamento roda depois do commit da troca; enquanto esta coluna
     * estiver preenchida a reconciliação tenta de novo.
     */
    @Column(name = "mp_preapproval_substituido_id", length = 50)
    private String mpPreapprovalSubstituidoId;

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

    /**
     * Dia do mês do ciclo de cobrança (1 a 31), fixado no primeiro pagamento aprovado de cada
     * preapproval: volta a null quando um preapproval novo é vinculado.
     */
    @Column(name = "dia_cobranca")
    private Integer diaCobranca;

    /**
     * Lock otimista: webhook, régua e reconciliação alteram a mesma linha em transações
     * concorrentes; sem versão a última escrita apagaria a renovação feita pela outra.
     */
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now(FusoHorario.ZONA);
        atualizadoEm = LocalDateTime.now(FusoHorario.ZONA);
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now(FusoHorario.ZONA);
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
        return dataVencimento != null && LocalDate.now(FusoHorario.ZONA).isAfter(dataVencimento);
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
     * Vincula um preapproval novo no lugar do atual. O atual (se houver) fica em
     * {@code mpPreapprovalSubstituidoId}, para ser cancelado no MP depois do commit.
     * O dia âncora é zerado: o MP cobra o preapproval novo a partir do dia em que o cartão
     * for cadastrado, então o próximo pagamento aprovado define a âncora nova.
     *
     * @param preapprovalId id do preapproval novo
     * @param mpStatus      status do preapproval novo no MP
     * @param initPoint     URL do checkout do preapproval novo
     * @param payerEmail    e-mail do pagador usado na criação
     */
    public void vincularNovoPreapproval(String preapprovalId, String mpStatus, String initPoint, String payerEmail) {
        if (temPreapproval()) {
            mpPreapprovalSubstituidoId = mpPreapprovalId;
        }
        mpPreapprovalId = preapprovalId;
        this.mpStatus = mpStatus;
        mpInitPoint = initPoint;
        mpPayerEmail = payerEmail;
        diaCobranca = null;
    }

    /**
     * Desvincula o preapproval atual sem pôr outro no lugar (link pendente com valor
     * antigo, cortesia). Ele fica em {@code mpPreapprovalSubstituidoId} para ser cancelado
     * no MP depois do commit; o próximo "Pagar" cria um preapproval novo.
     */
    public void desvincularPreapproval() {
        if (!temPreapproval()) {
            return;
        }
        mpPreapprovalSubstituidoId = mpPreapprovalId;
        mpPreapprovalId = null;
        mpStatus = null;
        mpInitPoint = null;
    }

    /**
     * Indica se há um preapproval trocado que ainda não foi confirmado como cancelado no MP.
     *
     * @return true se a coluna de substituído está preenchida
     */
    public boolean temPreapprovalSubstituidoPendente() {
        return mpPreapprovalSubstituidoId != null && !mpPreapprovalSubstituidoId.isBlank();
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
