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
    
    // Dados Vitais e Antropométricos
    private String peso;
    private String altura;
    private String temperatura;
    private String saturacao;
    private String pressao;
    private String frequenciaRespiratoria;
    private String frequenciaArterialSistolica;
    private String frequenciaArterialDiastolica;
    private String hemoglobina;
    
    // Dados Demográficos
    private Date dataNascimento;
    private String sexo;
    
    // Anamnese e Avaliação
    private String queixaPrincipal;
    private String anamnese;
    private String conduta;
    private String observacao;
    private String diagnostico;
    
    // Prescrição Médica
    private String modeloPrescricao;
    private String tituloPrescricao;
    private Date dataPrescricao;
    private String prescricao;
    
    // Exames
    private String modeloExame;
    private String tituloExame;
    private Date dataExame;
    private String exame;
    private String tempoDuracao;
    
    // Dados de Controle
    private Date dataFinalizado;
    private String codigoProntuario;
}
