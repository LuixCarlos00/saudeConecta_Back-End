package br.com.saudeConecta.domain.secretaria;

import br.com.saudeConecta.config.tenant.TenantAware;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "secretaria",
       uniqueConstraints = @UniqueConstraint(name = "uk_secretaria_org_cpf", columnNames = {"organizacao_id", "cpf"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Secretaria implements Serializable, TenantAware {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nome;

    // Único por clínica (changeset 063)
    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusSecretaria status = StatusSecretaria.ATIVO;

    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }

    @Override
    public void setOrganizacaoId(Long organizacaoId) {
        // Sem efeito: a organização é definida pela associação (campo "organizacao"); o setter existe só para cumprir a interface de tenant.
    }

    public boolean isAtivo() {
        return StatusSecretaria.ATIVO.equals(this.status);
    }
}
