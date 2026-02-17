package br.com.saudeConecta.domain.profissional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "especialidade", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"tipo_profissional_id", "nome"},
           name = "uk_especialidade_tipo_nome"
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Especialidade implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_profissional_id", nullable = false)
    @JsonIgnore
    private TipoProfissional tipoProfissional;
    
    @Column(nullable = false, length = 100)
    private String nome;
    
    @Column(length = 20)
    private String codigo;
    
    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Byte status = 1;
    
    public boolean isAtivo() {
        return status != null && status == 1;
    }
}
