package br.com.saudeConecta.domain.mensageria;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.infra.tenant.TenantAware;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidade que representa o registro de mensagens enviadas pelo sistema.
 * Armazena o template completo do email para reenvio em caso de falha.
 */
@Entity
@Table(name = "mensageria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Mensageria implements Serializable, TenantAware {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(name = "destinatario_entidade_id")
    private Long destinatarioEntidadeId;

    @Column(name = "destinatario_email", nullable = false, length = 200)
    private String destinatarioEmail;

    @Column(name = "destinatario_nome", length = 150)
    private String destinatarioNome;

    @Column(nullable = false, length = 300)
    private String assunto;

    @Column(name = "corpo_mensagem", columnDefinition = "TEXT", nullable = false)
    private String corpoMensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mensagem", nullable = false, length = 50)
    private TipoMensagem tipoMensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusMensagem status = StatusMensagem.PENDENTE;

    @Column(name = "erro_detalhe", columnDefinition = "TEXT")
    private String erroDetalhe;

    @Column(name = "tentativas")
    @Builder.Default
    private Integer tentativas = 0;

    @Column(name = "admin_notificado")
    @Builder.Default
    private Boolean adminNotificado = false;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }

    @Override
    public void setOrganizacaoId(Long organizacaoId) {
    }
}
