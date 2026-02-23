package br.com.saudeConecta.domain.paciente;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.infra.tenant.TenantAware;
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
@Builder
@AllArgsConstructor
@Table(name = "paciente")
@EqualsAndHashCode(of = "paciCodigo")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class Paciente implements Serializable, TenantAware {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "PaciCodigo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paciCodigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    @JsonIgnore
    private Organizacao organizacao;

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

    @Override
    public Long getOrganizacaoId() {
        return organizacao != null ? organizacao.getId() : null;
    }

    @Override
    public void setOrganizacaoId(Long organizacaoId) {
        if (this.organizacao == null) {
            this.organizacao = new Organizacao();
        }
        this.organizacao.setId(organizacaoId);
    }
}
