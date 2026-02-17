package br.com.saudeConecta.domain.endereco;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "endereco")
@EqualsAndHashCode(of = "endCodigo")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Endereco implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @Column(name = "EndCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long endCodigo;

    @Column(name = "EndNacionalidade", nullable = true)
    private String endNacionalidade;

    @Column(name = "EndUF", nullable = false)
    private String endUF;

    @Column(name = "EndMunicipio", nullable = false)
    private String endMunicipio;

    @Column(name = "EndBairro", nullable = true)
    private String endBairro;

    @Column(name = "EndCep", nullable = false)
    private String endCep;

    @Column(name = "EndRua", nullable = false)
    private String endRua;

    @Column(name = "EndNumero", nullable = false)
    private Long endNumero;

    @Column(name = "EndComplemento", nullable = true)
    private String endComplemento;
}
