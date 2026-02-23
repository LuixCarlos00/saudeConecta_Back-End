package br.com.saudeConecta.domain.historicodadospessoais;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.tenant.TenantAware;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_dados_pessoais", indexes = {
        @Index(name = "idx_historico_org_entidade_id", columnList = "organizacao_id, entidade, id_entidade"),
        @Index(name = "idx_historico_usuario", columnList = "organizacao_id, usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class HistoricoDadosPessoais implements Serializable, TenantAware {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    /**
     * Tipo da entidade alterada.
     * Ex: "PACIENTE", "PROFISSIONAL", "ADMIN"
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entidade", nullable = false, length = 50)
    private EntidadeTipo entidade;

    /**
     * ID do registro da entidade que foi alterado.
     * Ex: id do Paciente, id do Profissional etc.
     */
    @Column(name = "id_entidade", nullable = false)
    private Long idEntidade;

    /**
     * Nome do campo que foi alterado.
     * Ex: "nome", "email", "telefone", "cpf"
     */
    @Column(name = "campo", nullable = false, length = 100)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_novo", columnDefinition = "TEXT")
    private String valorNovo;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // ==========================================
    // TenantAware
    // ==========================================

    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }

    @Override
    public void setOrganizacaoId(Long organizacaoId) {
    }
}