package br.com.saudeConecta.presentation.dto.prontuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.sql.Date;

public record CadastrarProntuarioRequest(
        @Size(max = 10, message = "Altura deve ter no máximo 10 caracteres")
        String altura,
        
        @Size(max = 10, message = "Peso deve ter no máximo 10 caracteres")
        String peso,
        
        @Size(max = 10, message = "Temperatura deve ter no máximo 10 caracteres")
        String temperatura,
        

        @Size(max = 1, message = "Sexo deve ter no máximo 1 caractere")
        String sexo,
        
        @Size(max = 10, message = "Saturação deve ter no máximo 10 caracteres")
        String saturacao,
        
        @Size(max = 10, message = "Hemoglobina deve ter no máximo 10 caracteres")
        String hemoglobina,
        
        @Size(max = 20, message = "Pressão deve ter no máximo 20 caracteres")
        String pressao,
        
        @Size(max = 10, message = "Frequência respiratória deve ter no máximo 10 caracteres")
        String frequenciaRespiratoria,
        
        @Size(max = 10, message = "Frequência arterial sistólica deve ter no máximo 10 caracteres")
        String frequenciaArterialSistolica,
        
        @Size(max = 10, message = "Frequência arterial diastólica deve ter no máximo 10 caracteres")
        String frequenciaArterialDiastolica,
        
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
        String observacao,
        
        @Size(max = 500, message = "Conduta deve ter no máximo 500 caracteres")
        String conduta,
        
        @Size(max = 1000, message = "Anamnese deve ter no máximo 1000 caracteres")
        String anamnese,
        
        @Size(max = 200, message = "Queixa principal deve ter no máximo 200 caracteres")
        String queixaPrincipal,
        
        @Size(max = 500, message = "Diagnóstico deve ter no máximo 500 caracteres")
        String diagnostico,
        
        @Size(max = 100, message = "Modelo prescrição deve ter no máximo 100 caracteres")
        String modeloPrescricao,
        
        @Size(max = 100, message = "Título prescrição deve ter no máximo 100 caracteres")
        String tituloPrescricao,
        
        @Size(max = 20, message = "Data prescrição deve ter no máximo 20 caracteres")
        String dataPrescricao,
        
        @Size(max = 1000, message = "Prescrição deve ter no máximo 1000 caracteres")
        String prescricao,
        
        @NotNull(message = "Data finalizado não pode ser nula")
        Date dataFinalizado,
        
        @Size(max = 100, message = "Modelo exame deve ter no máximo 100 caracteres")
        String modeloExame,
        
        @Size(max = 100, message = "Título exame deve ter no máximo 100 caracteres")
        String tituloExame,
        
        @Size(max = 20, message = "Data exame deve ter no máximo 20 caracteres")
        String dataExame,
        
        @Size(max = 1000, message = "Exame deve ter no máximo 1000 caracteres")
        String exame,
        
        @Size(max = 50, message = "Tempo duração deve ter no máximo 50 caracteres")
        String tempoDuracao,
        
        @NotNull(message = "ID do médico não pode ser nulo")
        Long codigoMedico,
        
        @NotNull(message = "ID da consulta não pode ser nulo")
        Long consulta
) {
}
