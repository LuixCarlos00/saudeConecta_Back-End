package br.com.saudeConecta.endpoinst.secretaria.Entity;

import br.com.saudeConecta.endpoinst.secretaria.DTO.DadosCadastraSecretaria;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "secretaria")
@EqualsAndHashCode(of = "SecreCodigo")

public class Secretaria implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SecreCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long SecreCodigo;

    @Column(name = "SecreNome")
    private String SecreNome;

    @Column(name = "SecreStatus")
    private byte SecreStatus;

    @Column(name = "SecreDataCriacao")
    private Date SecreDataCriacao;

    @Column(name = "SecreEmail")
    private String SecreEmail;

    @Column(name = "SecreCodigoAtorizacao")
    private String SecreCodigoAtorizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SecreUsuario")
    private Usuario secreUsuario;


    public Secretaria(DadosCadastraSecretaria dados, Usuario usuario) {
        this.SecreNome = dados.SecreNome();
        this.SecreStatus = dados.SecreStatus();
        this.SecreDataCriacao = dados.SecreDataCriacao();
        this.secreUsuario = usuario;
        this.SecreEmail = dados.SecreEmail();

    }
}
