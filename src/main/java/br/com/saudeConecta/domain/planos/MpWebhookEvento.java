package br.com.saudeConecta.domain.planos;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registro bruto de uma notificação recebida do Mercado Pago, um por (tipo, dataId).
 *
 * O mesmo recurso é notificado várias vezes conforme muda de estado (fatura sem
 * pagamento → recusada → aprovada), então {@code processado} só fica true quando o
 * recurso atingiu um estado final; até lá cada nova notificação é reprocessada.
 */
@Entity
@Table(name = "mp_webhook_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class MpWebhookEvento implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Prefixo do motivo: pagamento aprovado chegou para assinatura CANCELADA (estorno ou reativação manual). */
    public static final String MOTIVO_PAGAMENTO_EM_CANCELADA = "PAGAMENTO_EM_CANCELADA";
    /** Prefixo do motivo: evento com erro passou do prazo de reprocessamento automático. */
    public static final String MOTIVO_EVENTO_EXPIRADO = "EVENTO_EXPIRADO";
    /** Prefixo do motivo: fatura aprovada de um preapproval sem assinatura local vinculada (ex.: preapproval substituído). */
    public static final String MOTIVO_PAGAMENTO_SEM_ASSINATURA = "PAGAMENTO_SEM_ASSINATURA";
    /** Prefixo do motivo: fatura aprovada de assinatura em cortesia (não renova; tratar manualmente). */
    public static final String MOTIVO_PAGAMENTO_EM_CORTESIA = "PAGAMENTO_EM_CORTESIA";
    private static final int TAMANHO_MAXIMO_MOTIVO = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @Column(name = "data_id", nullable = false, length = 60)
    private String dataId;

    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Builder.Default
    @Column(name = "processado", nullable = false)
    private Boolean processado = false;

    @Column(name = "erro", length = 500)
    private String erro;

    @Column(name = "recebido_em", nullable = false, updatable = false)
    private LocalDateTime recebidoEm;

    /** Quantas vezes o processamento falhou; base do backoff do reprocessamento. */
    @Builder.Default
    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    /** A partir de quando o job de reprocessamento pode tentar de novo. */
    @Column(name = "proxima_tentativa_em")
    private LocalDateTime proximaTentativaEm;

    /** Preenchida quando o evento com erro passou do prazo de reprocessamento automático. */
    @Column(name = "expirado_em")
    private LocalDateTime expiradoEm;

    /** Evento que precisa de tratamento manual (consultado em GET /assinaturas/pendencias-manuais). */
    @Builder.Default
    @Column(name = "requer_atencao", nullable = false)
    private Boolean requerAtencao = false;

    /** Por que o evento precisa de atenção: prefixo MOTIVO_* seguido do contexto. */
    @Column(name = "motivo_atencao", length = 255)
    private String motivoAtencao;

    @PrePersist
    protected void onCreate() {
        if (recebidoEm == null) {
            recebidoEm = LocalDateTime.now();
        }
    }

    /**
     * Registra um processamento sem erro, limpando falhas anteriores.
     *
     * @param estadoFinal true quando o recurso no Mercado Pago não muda mais de estado;
     *                    false mantém o evento aberto para as próximas notificações
     */
    public void registrarProcessamento(boolean estadoFinal) {
        this.processado = estadoFinal;
        this.erro = null;
        this.proximaTentativaEm = null;
    }

    /**
     * Marca o evento para tratamento manual. Não altera {@code processado}.
     *
     * @param motivo  um dos prefixos MOTIVO_*
     * @param detalhe contexto para quem for tratar (assinatura, organização, fatura)
     */
    public void marcarParaAtencao(String motivo, String detalhe) {
        String texto = detalhe != null ? motivo + ": " + detalhe : motivo;
        this.requerAtencao = true;
        this.motivoAtencao = texto.substring(0, Math.min(texto.length(), TAMANHO_MAXIMO_MOTIVO));
    }
}
