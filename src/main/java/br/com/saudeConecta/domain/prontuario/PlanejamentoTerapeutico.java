package br.com.saudeConecta.domain.prontuario;

import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.profissional.Profissional;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_planejamento_terapeutico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class PlanejamentoTerapeutico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prontuario_dentista_id", nullable = false)
    @JsonIgnore
    private ProntuarioDentista prontuarioDentista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(name = "data_procedimento", nullable = false)
    private LocalDate dataProcedimento;

    @Column(name = "procedimento_realizado", nullable = false, length = 500)
    private String procedimentoRealizado;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "token_assinatura")
    private String tokenAssinatura;

    @Column(name = "assinatura_base64", columnDefinition = "TEXT")
    private String assinaturaBase64;

    @Column(name = "data_assinatura")
    private LocalDateTime dataAssinatura;

    @Column(name = "status_assinatura", length = 20)
    @Builder.Default
    private String statusAssinatura = "PENDENTE";

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

    public boolean isPendente() {
        return "PENDENTE".equals(this.statusAssinatura);
    }

    public boolean isAssinado() {
        return "ASSINADO".equals(this.statusAssinatura);
    }
}
