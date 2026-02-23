package br.com.saudeConecta.domain.prontuario;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "prontuario_dentista_dente",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_prontuario_dente",
                columnNames = {"prontdentd_prontuario", "prontdentd_numero_fdi"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProntuarioDentistaDente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prontdentd_codigo")
    private Long codigo;

    /** FK para o prontuário odontológico pai */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontdentd_prontuario", nullable = false)
    private ProntuarioDentista prontuarioDentista;

    /** Número FDI do dente (11–18, 21–28, 31–38, 41–48) */
    @Column(name = "prontdentd_numero_fdi", nullable = false)
    private Integer numeroFdi;

    /**
     * Status clínico do dente.
     * Valores: sadio | cariado | obturado | ausente | protese | canal | fratura | implante
     */
    @Column(name = "prontdentd_status", length = 20, nullable = false)
    private String status;

    /** Observação livre do dentista sobre este dente */
    @Column(name = "prontdentd_observacao", columnDefinition = "TEXT")
    private String observacao;
}