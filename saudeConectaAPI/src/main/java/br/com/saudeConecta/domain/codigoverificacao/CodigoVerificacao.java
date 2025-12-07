package br.com.saudeConecta.domain.codigoverificacao;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "codigoverificacao")
@EqualsAndHashCode(of = "codVerificacaoCodigoID")
public class CodigoVerificacao implements Serializable {
    private static final long serialVersionUID = 1L;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codVerificacaoCodigoID;

    @Column(nullable = false, length = 6 )
    private String codVerificacaoCodigo;

    @Column(nullable = false)
    private LocalDateTime codVerificacaoTempo;

}
