package br.com.saudeConecta.domain.medico;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.endpoinst.medico.DTO.DadosCadastraMedico;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@EqualsAndHashCode(of = "medCodigo")
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
    private String medSexo;

    @Column(name = "MedDataNacimento", nullable = true)
    private Date medDataNacimento;

    @Column(name = "MedCrm", nullable = false)
    private String medCrm;

    @Column(name = "MedCpf", nullable = true)
    private String medCpf;

    @Column(name = "MedRg", nullable = true)
    private String medRg;

    @Column(name = "MedEmail", nullable = false)
    private String medEmail;

    @Column(name = "MedTelefone", nullable = true)
    private String medTelefone;

    @Column(name = "MedEspecialidade", nullable = true)
    private String medEspecialidade;

    @Column(name = "MedFormacoes", nullable = true)
    private String medFormacoes;

    @Column(name = "MedEmpresa", nullable = true)
    private String medEmpresa;

    @Column(name = "MedGraduacao", nullable = true)
    private String medGraduacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Usuario")
    @JsonIgnore
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Endereco")
    @JsonIgnore
    private Endereco endereco;

    @Column(name = "MedTempoDeConsulta", nullable = true)
    private String medTempoDeConsulta;

    public Medico(DadosCadastraMedico dados, Usuario usuario, Endereco endereco) {
        this.medNome = dados.MedNome();
        this.medSexo = dados.MedSexo();
        this.medDataNacimento = dados.MedDataNacimento();
        this.medCrm = dados.MedCrm();
        this.medCpf = dados.MedCpf();
        this.medRg = dados.MedRg();
        this.medEmail = dados.MedEmail();
        this.medTelefone = dados.MedTelefone();
        this.medEspecialidade = dados.MedEspecialidade();
        this.medFormacoes = dados.MedFormacoes();
        this.medEmpresa = dados.MedEmpresa();
        this.medGraduacao = dados.MedGraduacao();
        this.usuario = usuario;
        this.endereco = endereco;
    }
}
