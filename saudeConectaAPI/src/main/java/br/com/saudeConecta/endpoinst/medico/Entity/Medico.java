package br.com.saudeConecta.endpoinst.medico.Entity;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.medico.DTO.DadosCadastraMedico;
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
@Table(name = "medico")
@EqualsAndHashCode(of = "idMedico")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Medico implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "MedCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long MedCodigo;

    @Column(name = "MedNome", nullable = false)
    private String MedNome;

    @Column(name = "MedSexo", nullable = false)
    private String MedSexo;

    @Column(name = "MedDataNacimento", nullable = true)
    private Date MedDataNacimento;

    @Column(name = "MedCrm", nullable = false)
    private String MedCrm;

    @Column(name = "MedCpf", nullable = false)
    private String MedCpf;

    @Column(name = "MedRg", nullable = false)
    private String MedRg;

    @Column(name = "MedEmail", nullable = false)
    private String medEmail;


    @Column(name = "MedTelefone", nullable = true)
    private String MedTelefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario")
    private Usuario usuario ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Endereco")
    private Endereco endereco;


    public Medico(String medNome, String medSexo, Date medDataNacimento, String medCrm, String medCpf,
                  String medRg,    String medTelefone,String medEmail,Usuario usuario, Endereco endereco) {
        this.MedNome=medNome;
        this.MedSexo=medSexo;
        this.MedDataNacimento=medDataNacimento;
        this.MedCrm=medCrm;
        this.MedCpf=medCpf;
        this.MedRg=medRg;
        this.MedTelefone=medTelefone;
        this.usuario = usuario ;
        this.endereco=endereco;
        this.medEmail = medEmail;


    }

    public Medico(DadosCadastraMedico dados, Usuario usuario, Endereco endereco) {

        this.MedNome= dados.MedNome();
        this.MedSexo= dados.MedSexo();
        this.MedDataNacimento=getMedDataNacimento();
        this.MedCrm= dados.MedCrm();
        this.MedCpf= dados.MedCpf();
        this.MedRg= dados.MedRg();
        this.medEmail = dados.MedEmail();
        this.MedTelefone= dados.MedTelefone();
        this.usuario = usuario ;
        this.endereco=endereco;

    }
}
