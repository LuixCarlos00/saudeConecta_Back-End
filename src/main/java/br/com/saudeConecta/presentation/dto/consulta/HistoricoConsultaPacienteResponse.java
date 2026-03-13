package br.com.saudeConecta.presentation.dto.consulta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * DTO unificado de resposta para histórico completo de consultas do paciente.
 * Suporta prontuário médico e prontuário de dentista.
 * O campo tipoProntuario indica qual conjunto de dados está preenchido ("MEDICO" ou "DENTISTA").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoConsultaPacienteResponse {

    // ── Discriminador ────────────────────────────────────────────────────────
    private String tipoProntuario;

    // ── Dados da Consulta ────────────────────────────────────────────────────
    private Long consultaId;
    private LocalDateTime dataHora;
    private Integer duracaoMinutos;
    private String observacoes;
    private BigDecimal valor;
    private String status;
    private String motivoCancelamento;

    // ── Dados do Paciente ────────────────────────────────────────────────────
    private Long pacienteId;
    private String pacienteNome;
    private String pacienteCpf;
    private Date pacienteDataNascimento;
    private String pacienteTelefone;

    // ── Dados do Profissional ────────────────────────────────────────────────
    private Long profissionalId;
    private String profissionalNome;
    private String profissionalEspecialidade;
    private String profissionalCrm;

    // ── Dados comuns do Prontuário ───────────────────────────────────────────
    private Long prontuarioId;
    private String codigoProntuario;
    private String queixaPrincipal;
    private String anamnese;
    private String observacao;
    private String diagnostico;
    private String peso;
    private String altura;
    private String temperatura;
    private String pressao;
    private String tituloPrescricao;
    private String dataPrescricao;
    private String prescricao;
    private String tituloExame;
    private String dataExame;
    private String tempoDuracao;

    // ── Campos exclusivos do Prontuário Médico ──────────────────────────────
    private String sexo;
    private String saturacao;
    private String hemoglobina;
    private String frequenciaRespiratoria;
    private String frequenciaArterialSistolica;
    private String frequenciaArterialDiastolica;
    private String conduta;
    private String modeloPrescricao;
    private String modeloExame;
    private String exame;
    private Date dataFinalizado;

    // ── Campos exclusivos do Prontuário Dentista ────────────────────────────
    private String higieneBucal;
    private String condicaoGengival;
    private String oclusal;
    private String atm;
    private String planoTratamento;
    private String procedimentos;
    private String orientacoes;
    private String responsavel;
    private LocalDate inicioTratamento;
    private LocalDate terminoTratamento;
    private String interrupcao;
    private String pulso;
    private String edema;
    private String facies;
    private String linfonodos;
    private String labios;
    private String mucosas;
    private String soalhoBucal;
    private String palato;
    private String orofaringe;
    private String lingua;
    private String gengiva;
    private String habitosNocivos;
    private String portadorAparelho;
    private String exameOutros;
    private String tussTexto;
    private String cidTexto;
    private String solicitacaoExameTexto;
    private LocalDate dataFinalizadoDentista;
    private List<DenteResponse> dentes;
    private List<PlanejamentoResponse> planejamentos;

    // ── Sub-DTOs ─────────────────────────────────────────────────────────────

    /**
     * DTO representando um dente do odontograma.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DenteResponse {
        private Long codigo;
        private Integer numeroFdi;
        private String status;
        private String observacao;
    }

    /**
     * DTO representando um planejamento terapêutico.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanejamentoResponse {
        private Long id;
        private LocalDate dataProcedimento;
        private String procedimentoRealizado;
        private BigDecimal valor;
        private String statusAssinatura;
    }
}
