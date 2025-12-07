package br.com.saudeConecta.domain.administrador;

import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.endpoinst.administrador.DTO.DadosCadastraAdministrador;
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
@Table(name = "administrador")
@EqualsAndHashCode(of = "admCodigo")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Administrador implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "AdmCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long admCodigo;

    @Column(name = "AdmNome")
    private String admNome;

    @Column(name = "AdmStatus")
    private byte admStatus;

    @Column(name = "AdmDataCriacao")
    private Date admDataCriacao;

    @Column(name = "AdmEmail")
    private String admEmail;

    @Column(name = "AdmCodigoAtorizacao")
    private String admCodigoAtorizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmUsuario")
    private Usuario admUsuario;

    public Administrador(DadosCadastraAdministrador dados, Usuario usuario) {
        this.admNome = dados.AdmNome();
        this.admStatus = dados.AdmStatus();
        this.admDataCriacao = dados.AdmDataCriacao();
        this.admEmail = dados.AdmEmail();
        this.admUsuario = usuario;
    }
}
