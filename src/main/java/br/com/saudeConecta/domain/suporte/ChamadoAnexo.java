package br.com.saudeConecta.domain.suporte;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidade que representa um arquivo de imagem anexado a um chamado de suporte.
 * O conteudo e armazenado em base64 para simplificar o consumo direto pelo frontend.
 */
@Entity
@Table(name = "chamado_suporte_anexo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class ChamadoAnexo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id", nullable = false, columnDefinition = "VARCHAR(20)")
    @JsonIgnore
    private ChamadoSuporte chamado;

    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;

    @Column(name = "tipo_conteudo", length = 100)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "conteudo_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String conteudoBase64;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
}
