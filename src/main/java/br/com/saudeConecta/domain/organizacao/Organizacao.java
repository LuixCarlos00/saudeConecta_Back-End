package br.com.saudeConecta.domain.organizacao;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.planos.AssinaturaTenant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Organizacao implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 150)
    private String nome;
    
    @Column(name = "razao_social", length = 200)
    private String razaoSocial;
    
    @Column(unique = true, length = 18)
    private String cnpj;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoOrganizacao tipo;
    
    @Column(length = 150)
    private String email;
    
    @Column(length = 20)
    private String telefone;
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;
    
    @OneToMany(mappedBy = "organizacao", fetch = FetchType.LAZY)
    private List<AssinaturaTenant> assinaturas;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusOrganizacao status = StatusOrganizacao.ATIVO;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isAtivo() {
        return StatusOrganizacao.ATIVO.equals(this.status);
    }
}
