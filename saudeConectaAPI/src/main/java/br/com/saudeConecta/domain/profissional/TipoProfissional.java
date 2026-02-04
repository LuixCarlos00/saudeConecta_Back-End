package br.com.saudeConecta.domain.profissional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "tipo_profissional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class TipoProfissional implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 20)
    private String codigo;
    
    @Column(nullable = false, length = 100)
    private String nome;
    
    @Column(nullable = false, length = 20)
    private String conselho;
    
    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Byte status = 1;
    
    public boolean isAtivo() {
        return status != null && status == 1;
    }
    
    public boolean isMedico() {
        return "MEDICO".equals(this.codigo);
    }
    
    public boolean isDentista() {
        return "DENTISTA".equals(this.codigo);
    }
}
