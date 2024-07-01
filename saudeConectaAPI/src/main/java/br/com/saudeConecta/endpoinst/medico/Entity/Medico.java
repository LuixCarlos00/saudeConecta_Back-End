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
    private Long medCodigo;

    @Column(name = "MedNome", nullable = false)
    private String medNome;

    @Column(name = "MedSexo", nullable = false)
    private String MedSexo;

    @Column(name = "MedDataNacimento", nullable = true)
    private Date MedDataNacimento;

    @Column(name = "MedCrm", nullable = false)
    private String medCrm;

    @Column(name = "MedCpf", nullable = true)
    private String MedCpf;

    @Column(name = "MedRg", nullable = true)
    private String MedRg;

    @Column(name = "MedEmail", nullable = false)
    private String medEmail;

    @Column(name = "MedTelefone", nullable = true)
    private String MedTelefone;

    @Column(name = "MedEspecialidade", nullable = true)
    private String medEspecialidade;

    @Column(name = "MedFormacoes", nullable = true)
    private String MedFormacoes;

    @Column(name = "MedEmpresa", nullable = true)
    private String MedEmpresa;

    @Column(name = "MedGraduacao", nullable = true)
    private String MedGraduacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Endereco")
    private Endereco endereco;


    public Medico(String medNome, String medSexo, Date medDataNacimento, String medCrm, String medCpf,
                  String medRg, String medEspecialidade, String medTelefone, String medEmail, String MedEmpresa,
                  String MedGraduacao, String MedFormacoes , Usuario usuario, Endereco endereco) {
        this.medNome = medNome;
        this.MedSexo = medSexo;
        this.MedDataNacimento = medDataNacimento;
        this.medCrm = medCrm;
        this.MedCpf = medCpf;
        this.medEspecialidade = medEspecialidade;
        this.MedRg = medRg;
        this.MedTelefone = medTelefone;
        this.usuario = usuario;
        this.endereco = endereco;
        this.medEmail = medEmail;
        this.MedEmpresa = MedEmpresa;
        this.MedGraduacao = MedGraduacao;
        this.MedFormacoes = MedFormacoes;


    }

    public Medico(DadosCadastraMedico dados, Usuario usuario, Endereco endereco) {

        this.medNome = dados.MedNome();
        this.MedSexo = dados.MedSexo();
        this.MedDataNacimento = getMedDataNacimento();
        this.medCrm = dados.MedCrm();
        this.MedCpf = dados.MedCpf();
        this.MedRg = dados.MedRg();
        this.medEspecialidade = dados.MedEspecialidade();
        this.medEmail = dados.MedEmail();
        this.MedTelefone = dados.MedTelefone();
        this.usuario = usuario;
        this.endereco = endereco;
        this.MedFormacoes = MedFormacoes;
        this.MedGraduacao = MedGraduacao;
        this.MedEmpresa = MedEmpresa;

    }
}
