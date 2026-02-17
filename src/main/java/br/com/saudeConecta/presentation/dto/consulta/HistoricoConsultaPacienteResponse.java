package br.com.saudeConecta.presentation.dto.consulta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * DTO de resposta para histórico completo de consultas do paciente
 * Inclui dados da consulta, paciente, profissional e prontuário
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoConsultaPacienteResponse {
    
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
    
    // Dados do Prontuário (se existir)
    private Long prontuarioId;
    private String prontPeso;
    private String prontAltura;
    private String prontTemperatura;
    private String prontSaturacao;
    private String prontPressao;
    private String prontFrequenciaRespiratoria;
    private String prontFrequenciaArterialSistolica;
    private String prontFrequenciaArterialDiastolica;
    private String prontQueixaPrincipal;
    private String prontAnamnese;
    private String prontDiagnostico;
    private String prontPrescricao;
    private String prontExame;
    private String prontObservacao;
    private String prontTempoDuracao;
}
