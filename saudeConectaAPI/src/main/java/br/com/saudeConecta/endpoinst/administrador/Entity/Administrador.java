package br.com.saudeConecta.endpoinst.administrador.Entity;

import br.com.saudeConecta.endpoinst.administrador.DTO.DadosCadastraAdministrador;
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
@Table(name = "administrador")
@EqualsAndHashCode(of = "AdmCodigo")

public class Administrador implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "AdmCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long AdmCodigo;

    @Column(name = "AdmNome")
    private String AdmNome;

    @Column(name = "AdmStatus")
    private byte AdmStatus;

    @Column(name = "AdmDataCriacao")
    private Date AdmDataCriacao;

    @Column(name = "AdmEmail")
    private String admEmail;

    @Column(name = "AdmCodigoAtorizacao")
    private String admCodigoAtorizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmUsuario")
    private Usuario admUsuario;


    public Administrador(DadosCadastraAdministrador dados, Usuario usuario) {
        this.AdmNome = dados.AdmNome();
        this.AdmStatus = dados.AdmStatus();
        this.AdmDataCriacao = dados.AdmDataCriacao();
        this.admUsuario = usuario;
        this.admEmail = dados.AdmEmail();

    }
}
