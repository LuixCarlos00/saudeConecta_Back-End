package br.com.saudeConecta.presentation.dto.consulta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * DTO de resposta para histórico completo de consultas odontológicas do paciente
 * Inclui dados da consulta, paciente, profissional e prontuário odontológico
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoConsultaDentistaResponse {

    // Dados da Consulta
    private Long consultaId;
    private LocalDateTime dataHora;
    private Integer duracaoMinutos;
    private String observacoes;
    private BigDecimal valor;
    private String status;
    private String motivoCancelamento;

    // Dados do Paciente
    private Long pacienteId;
    private String pacienteNome;
    private String pacienteCpf;
    private Date pacienteDataNascimento;
    private String pacienteTelefone;

    // Dados do Profissional
    private Long profissionalId;
    private String profissionalNome;
    private String profissionalEspecialidade;
    private String profissionalCrm;

    // Dados do Prontuário Odontológico (se existir)
    private Long prontuarioId;

    // Anamnese e Avaliação Odontológica
    private String queixaPrincipal;
    private String anamnese;
    private String observacao;
    private String diagnostico;

    // Avaliação Bucal
    private String higieneBucal;
    private String condicaoGengival;
    private String oclusal;
    private String atm;

    // Plano de Tratamento
    private String planoTratamento;
    private String procedimentos;
    private String orientacoes;

    // Prescrição
    private String tituloPrescricao;
    private String dataPrescricao;
    private String prescricao;

    // Exames
    private String tituloExame;
    private String dataExame;

    // Dados de Controle
    private String tempoDuracao;
    private Date dataFinalizado;

    // Odontograma — dentes do prontuário
    private List<DenteResponse> dentes;

    /**
     * DTO representando um dente do odontograma
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
}
