package br.com.saudeConecta.domain.suporte;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.tenant.TenantAware;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um chamado tecnico aberto por um usuario da organizacao.
 * Isolada por tenant atraves do relacionamento com Organizacao.
 */
@Entity
@Table(name = "chamado_suporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class ChamadoSuporte implements Serializable, TenantAware {

    private static final long serialVersionUID = 1L;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @Column(name = "id", length = 20, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_criador_id", nullable = false)
    @JsonIgnore
    private Usuario usuarioCriador;

    @Column(name = "criador_nome", length = 150)
    private String criadorNome;

    @Column(name = "criador_email", length = 200)
    private String criadorEmail;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaChamado categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadeChamado prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusChamado status = StatusChamado.EM_ANALISE;

    @Column(name = "previsao_atendimento")
    private LocalDate previsaoAtendimento;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChamadoAnexo> anexos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = gerarIdentificador();
        }
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        if (status == null) {
            status = StatusChamado.EM_ANALISE;
        }
        if (previsaoAtendimento == null && prioridade != null) {
            previsaoAtendimento = LocalDate.now().plusDays(prioridade.getPrazoPrevistoDias());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Adiciona um anexo ao chamado mantendo a consistencia bidirecional.
     *
     * @param anexo anexo a ser vinculado ao chamado
     */
    public void adicionarAnexo(ChamadoAnexo anexo) {
        if (anexo == null) {
            return;
        }
        anexo.setChamado(this);
        this.anexos.add(anexo);
    }

    /**
     * Gera um identificador aleatorio no formato 999999-999999-99999.
     *
     * @return identificador aleatorio do chamado
     */
    public static String gerarIdentificador() {
        return String.format("%06d-%06d-%05d",
                RANDOM.nextInt(1_000_000),
                RANDOM.nextInt(1_000_000),
                RANDOM.nextInt(100_000));
    }

    /**
     * @return protocolo legivel do chamado (ex.: #SC-230423-324234-32423)
     */
    public String getProtocolo() {
        return id != null ? "#SC-" + id : null;
    }

    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }

    @Override
    public void setOrganizacaoId(Long organizacaoId) {
    }
}
