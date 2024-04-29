package br.com.saudeConecta.endpoinst.endereco.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "endereco")
@EqualsAndHashCode(of = "EndCodigo")
 public class Endereco implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "EndCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long EndCodigo;

    @Column(name = "EndNacionalidade",nullable = false)
    private String EndNacionalidade;

    @Column(name = "EndEstado",nullable = false)
    private String EndEstado;

    @Column(name = "EndUF",nullable = false)
    private String EndUF;

    @Column(name = "EndMunicipio",nullable = false)
    private String endMunicipio;


    @Column(name = "EndBairro",nullable = true)
    private String EndBairro;

    @Column(name = "EndCep",nullable = false)
    private String EndCep;

    @Column(name = "EndRua",nullable = false)
    private String EndRua;

    @Column(name = "EndNumero",nullable = false)
    private Long EndNumero;

    @Column(name = "EndComplemento",nullable = true)
    private String EndComplemento;


    public Endereco(String endNacionalidade, String endEstado, String endUF, String endMunicipio,
                    String endBairro, String endCep, String endRua, Long endNumero, String endComplemento) {

        this.EndNacionalidade=endNacionalidade;
        this.EndEstado=endEstado;
        this.EndUF=endUF;
        this.endMunicipio=endMunicipio;
        this.EndBairro=endBairro;
        this.EndCep=endCep;
        this.EndRua=endRua;
        this.EndNumero=endNumero;
        this.EndComplemento=endComplemento;

    }
}
