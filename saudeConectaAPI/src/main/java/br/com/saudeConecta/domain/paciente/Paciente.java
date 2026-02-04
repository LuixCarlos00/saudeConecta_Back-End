package br.com.saudeConecta.domain.paciente;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.presentation.dto.paciente.CadastrarPacienteRequest;
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
@Table(name = "paciente")
@EqualsAndHashCode(of = "paciCodigo")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Paciente implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "PaciCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paciCodigo;

    @Column(name = "PaciNome", nullable = false)
    private String paciNome;

    @Column(name = "PaciSexo", nullable = false)
    private String paciSexo;

    @Column(name = "PaciDataNacimento", nullable = true)
    private Date paciDataNacimento;

    @Column(name = "PaciCpf", nullable = true)
    private String paciCpf;

    @Column(name = "PaciRg", nullable = true)
    private String paciRg;

    @Column(name = "PaciEmail", nullable = false)
    private String paciEmail;

    @Column(name = "PaciTelefone", nullable = true)
    private String paciTelefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Endereco")
    @JsonIgnore
    private Endereco endereco;

    @Column(name = "PaciStatus", nullable = false)
    private String paciStatus;

    public Paciente(CadastrarPacienteRequest dados, Endereco endereco) {
        this.paciNome = dados.paciNome();
        this.paciSexo = dados.paciSexo();
        this.paciDataNacimento = dados.paciDataNacimento() != null ? 
            Date.valueOf(dados.paciDataNacimento()) : null;
        this.paciCpf = dados.paciCpf();
        this.paciRg = dados.paciRg();
        this.paciEmail = dados.paciEmail();
        this.paciTelefone = dados.paciTelefone();
        this.endereco = endereco;
        this.paciStatus = dados.paciStatus();
    }
}
