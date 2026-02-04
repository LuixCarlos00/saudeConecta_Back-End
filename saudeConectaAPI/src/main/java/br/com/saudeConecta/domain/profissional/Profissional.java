package br.com.saudeConecta.domain.profissional;

import br.com.saudeConecta.domain.common.Sexo;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infra.tenant.TenantAware;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "profissional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Profissional implements Serializable, TenantAware {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_profissional_id", nullable = false)
    private TipoProfissional tipoProfissional;
    
    @Column(nullable = false, length = 150)
    private String nome;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sexo sexo;
    
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    
    @Column(name = "registro_conselho", nullable = false, length = 20)
    private String registroConselho;
    
    @Column(unique = true, length = 14)
    private String cpf;
    
    @Column(length = 20)
    private String rg;
    
    @Column(length = 150)
    private String email;
    
    @Column(length = 20)
    private String telefone;
    
    @Column(length = 200)
    private String formacao;
    
    @Column(length = 200)
    private String instituicao;
    
    @Column(name = "tempo_consulta_minutos")
    private Integer tempoConsultaMinutos = 30;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id")
    @JsonIgnore
    private Endereco endereco;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusProfissional status = StatusProfissional.ATIVO;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "profissional_especialidade",
        joinColumns = @JoinColumn(name = "profissional_id"),
        inverseJoinColumns = @JoinColumn(name = "especialidade_id")
    )
    @Builder.Default
    private Set<Especialidade> especialidades = new HashSet<>();
    
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
    
    @Override
    public Long getOrganizacaoId() {
        return this.organizacao != null ? this.organizacao.getId() : null;
    }
    
    @Override
    public void setOrganizacaoId(Long organizacaoId) {
    }
    
    public boolean isAtivo() {
        return StatusProfissional.ATIVO.equals(this.status);
    }
    
    public boolean isMedico() {
        return this.tipoProfissional != null && this.tipoProfissional.isMedico();
    }
    
    public boolean isDentista() {
        return this.tipoProfissional != null && this.tipoProfissional.isDentista();
    }
    
    public String getConselhoFormatado() {
        if (this.tipoProfissional == null) return this.registroConselho;
        return this.tipoProfissional.getConselho() + " " + this.registroConselho;
    }
    
    public void addEspecialidade(Especialidade especialidade) {
        this.especialidades.add(especialidade);
    }
    
    public void removeEspecialidade(Especialidade especialidade) {
        this.especialidades.remove(especialidade);
    }
}
