package br.com.saudeConecta.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracao_grafico_dashboard")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoGraficoDashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizacao_id", nullable = false)
    private Long organizacaoId;

    @Column(name = "tipo_grafico", nullable = false, length = 100)
    private String tipoGrafico;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "ordem_exibicao")
    private Integer ordemExibicao;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        if (ativo == null) {
            ativo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
