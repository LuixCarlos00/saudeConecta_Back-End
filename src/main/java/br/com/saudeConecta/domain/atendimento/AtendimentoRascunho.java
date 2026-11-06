package br.com.saudeConecta.domain.atendimento;

import br.com.saudeConecta.domain.FusoHorario;
import br.com.saudeConecta.domain.consulta.Consulta;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * O que o profissional já digitou na tela de atendimento antes de "Finalizar consulta".
 *
 * <p>Fica fora de {@code prontuario}/{@code prontuario_dentista}, que só recebem prontuário finalizado.
 * Um por consulta (UNIQUE em {@code consulta_id}); apagado na mesma transação que grava o prontuário.
 * {@code dados} é JSON opaco para o back: o formato é do front.</p>
 */
@Entity
@Table(name = "atendimento_rascunho",
       uniqueConstraints = @UniqueConstraint(name = "uk_atendimento_rascunho_consulta", columnNames = "consulta_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AtendimentoRascunho implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false, updatable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, updatable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id", nullable = false, updatable = false)
    private Profissional profissional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoAtendimentoRascunho tipo;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String dados;

    @Column(name = "tempo_acumulado_segundos", nullable = false)
    private Integer tempoAcumuladoSegundos;

    /** Lock otimista: o front devolve a versão que leu e recebe 409 se outra aba gravou depois. */
    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    // Segundos inteiros: a coluna DATETIME não guarda fração e a resposta precisa bater com o que o GET devolve depois.
    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now(FusoHorario.ZONA).truncatedTo(ChronoUnit.SECONDS);
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now(FusoHorario.ZONA).truncatedTo(ChronoUnit.SECONDS);
    }
}
