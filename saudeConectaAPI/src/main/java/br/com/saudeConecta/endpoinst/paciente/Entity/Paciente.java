package br.com.saudeConecta.endpoinst.paciente.Entity;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.paciente.DTO.DadosCadastraPaciente;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

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
    private Long paciCodigo;

    @Column(name = "PaciNome", nullable = false)
    private String paciNome;

    @Column(name = "PaciSexo", nullable = false)
    private String PaciSexo;

    @Column(name = "PaciDataNacimento", nullable = true)
    private Date PaciDataNacimento;

    @Column(name = "PaciCpf", nullable = false)
    private String paciCpf;

    @Column(name = "PaciRg", nullable = false)
    private String paciRg;

    @Column(name = "PaciEmail", nullable = false)
    private String paciEmail;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "Usuario")
//    private Usuario usuario;

    @Column(name = "PaciTelefone", nullable = true)
    private String paciTelefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Endereco")
    private Endereco endereco;




    public Paciente(@NotNull DadosCadastraPaciente dados , Endereco dadosEndereco) {

        this.paciNome = dados.PaciNome();
        this.PaciSexo= dados.PaciSexo();
        this.PaciDataNacimento=dados.PaciDataNacimento();
        this.paciCpf = dados.PaciCpf();
        this.paciRg = dados.PaciRg();
        this.paciEmail = dados.PaciEmail();
        //this.usuario = dadosIdUsuario;
        this.paciTelefone = dados.PaciTelefone();
        this.endereco=dadosEndereco;
    }



}
