package br.com.saudeConecta.presentation.dto.prontuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.sql.Date;

public record CadastrarProntuarioRequest(
        @NotBlank(message = "Altura não pode ser vazia")
        @Size(max = 10, message = "Altura deve ter no máximo 10 caracteres")
        String prontAltura,
        
        @NotBlank(message = "Peso não pode ser vazio")
        @Size(max = 10, message = "Peso deve ter no máximo 10 caracteres")
        String prontPeso,
        
        @NotBlank(message = "Temperatura não pode ser vazia")
        @Size(max = 10, message = "Temperatura deve ter no máximo 10 caracteres")
        String prontTemperatura,
        
        @NotBlank(message = "Data de nascimento não pode ser vazia")
        @Size(max = 20, message = "Data de nascimento deve ter no máximo 20 caracteres")
        String prontDataNacimento,
        
        @Size(max = 1, message = "Sexo deve ter no máximo 1 caractere")
        String prontSexo,
        
        @Size(max = 10, message = "Saturação deve ter no máximo 10 caracteres")
        String prontSaturacao,
        
        @Size(max = 10, message = "Hemoglobina deve ter no máximo 10 caracteres")
        String prontHemoglobina,
        
        @Size(max = 20, message = "Pressão deve ter no máximo 20 caracteres")
        String prontPressao,
        
        @Size(max = 10, message = "Frequência respiratória deve ter no máximo 10 caracteres")
        String prontFrequenciaRespiratoria,
        
        @Size(max = 10, message = "Frequência arterial sistólica deve ter no máximo 10 caracteres")
        String prontFrequenciaArterialSistolica,
        
        @Size(max = 10, message = "Frequência arterial diastólica deve ter no máximo 10 caracteres")
        String prontFrequenciaArterialDiastolica,
        
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String prontObservacao,
        
        @Size(max = 500, message = "Consulta deve ter no máximo 500 caracteres")
        String prontCondulta,
        
        @Size(max = 1000, message = "Anamnese deve ter no máximo 1000 caracteres")
        String prontAnamnese,
        
        @Size(max = 200, message = "Queixa principal deve ter no máximo 200 caracteres")
        String prontQueixaPricipal,
        
        @Size(max = 500, message = "Diagnóstico deve ter no máximo 500 caracteres")
        String prontDiagnostico,
        
        @Size(max = 100, message = "Modelo prescrição deve ter no máximo 100 caracteres")
        String prontModeloPrescricao,
        
        @Size(max = 100, message = "Título prescrição deve ter no máximo 100 caracteres")
        String prontTituloPrescricao,
        
        @Size(max = 20, message = "Data prescrição deve ter no máximo 20 caracteres")
        String prontDataPrescricao,
        
        @Size(max = 1000, message = "Prescrição deve ter no máximo 1000 caracteres")
        String prontPrescricao,
        
        @NotNull(message = "Data finalizado não pode ser nula")
        Date prontDataFinalizado,
        
        @Size(max = 100, message = "Modelo exame deve ter no máximo 100 caracteres")
        String prontModeloExame,
        
        @Size(max = 100, message = "Título exame deve ter no máximo 100 caracteres")
        String prontTituloExame,
        
        @Size(max = 20, message = "Data exame deve ter no máximo 20 caracteres")
        String prontDataExame,
        
        @Size(max = 1000, message = "Exame deve ter no máximo 1000 caracteres")
        String prontExame,
        
        @Size(max = 50, message = "Tempo duração deve ter no máximo 50 caracteres")
        String prontTempoDuracao,
        
        @NotNull(message = "ID do médico não pode ser nulo")
        Long prontMedico,
        
        @NotNull(message = "ID da consulta não pode ser nulo")
        Long prontConsulta
) {
}
