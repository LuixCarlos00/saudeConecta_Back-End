package br.com.saudeConecta.endpoinst.paciente.Entity;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
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
@Table(name = "paciente")
@EqualsAndHashCode(of = "PaciCodigo")

public class Paciente implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "PaciCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long PaciCodigo;

    @Column(name = "PaciNome", nullable = false)
    private String PaciNome;

    @Column(name = "PaciSexo", nullable = false)
    private String PaciSexo;

    @Column(name = "PaciDataNacimento", nullable = true)
    private Date PaciDataNacimento;

    @Column(name = "PaciCpf", nullable = false)
    private String PaciCpf;

    @Column(name = "PaciRg", nullable = false)
    private String PaciRg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario")
    private Usuario usuario;

    @Column(name = "PaciTelefone", nullable = true)
    private String PaciTelefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Endereco")
    private Endereco endereco;


//    public Paciente(String paciNome, String paciSexo, Date paciDataNacimento, String paciCpf, String paciRg,
//                    Usuario usuario, String paciTelefone, Endereco endereco) {
//
//        this.PaciNome=paciNome;
//        this.PaciSexo=paciSexo;
//        this.PaciDataNacimento=paciDataNacimento;
//        this.PaciCpf=paciCpf;
//        this.PaciRg=paciRg;
//        this.usuario = usuario;
//        this.PaciTelefone=paciTelefone;
//        this.endereco=endereco;
//
//    }

    public Paciente(DadosCadastraPaciente dados, Usuario dadosIdUsuario, Endereco dadosEndereco) {

        this.PaciNome= dados.PaciNome();
        this.PaciSexo= dados.PaciSexo();
        this.PaciDataNacimento=dados.PaciDataNacimento();
        this.PaciCpf= dados.PaciCpf();
        this.PaciRg= dados.PaciRg();
        this.usuario = dadosIdUsuario;
        this.PaciTelefone= dados.PaciTelefone();
        this.endereco=dadosEndereco;
    }
}
