package br.com.saudeConecta.domain.admin;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_organizacao",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"organizacao_id", "usuario_id"},
           name = "uk_admin_org"
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class AdminOrganizacao implements Serializable {
    
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
    
    @Column(nullable = false, length = 150)
    private String nome;
    
    @Column(length = 100)
    private String cargo;
    
    @Column(length = 150)
    private String email;
    
    @Column(name = "is_owner", columnDefinition = "TINYINT DEFAULT 0")
    private Boolean isOwner = false;
    
    @Column(columnDefinition = "JSON")
    private String permissoes;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusAdmin status = StatusAdmin.ATIVO;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public boolean isAtivo() {
        return StatusAdmin.ATIVO.equals(this.status);
    }
    
    public enum StatusAdmin {
        ATIVO, INATIVO
    }
}
