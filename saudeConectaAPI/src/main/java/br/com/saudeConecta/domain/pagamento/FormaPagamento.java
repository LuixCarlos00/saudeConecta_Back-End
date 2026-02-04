package br.com.saudeConecta.domain.pagamento;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "forma_pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class FormaPagamento implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    @JsonIgnore
    private Organizacao organizacao;
    
    @Column(nullable = false, length = 50)
    private String nome;
    
    @Column(length = 20)
    private String codigo;
    
    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Byte status = 1;
    
    public boolean isGlobal() {
        return organizacao == null;
    }
    
    public boolean isAtivo() {
        return status != null && status == 1;
    }
}
