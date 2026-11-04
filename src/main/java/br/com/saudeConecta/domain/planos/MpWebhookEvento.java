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
    }

    /**
     * Registra a falha de processamento mantendo o evento como não processado.
     *
     * @param mensagem descrição do erro (truncada em 500 caracteres)
     */
    public void marcarErro(String mensagem) {
        this.processado = false;
        this.erro = mensagem == null ? null : mensagem.substring(0, Math.min(mensagem.length(), 500));
    }
}
