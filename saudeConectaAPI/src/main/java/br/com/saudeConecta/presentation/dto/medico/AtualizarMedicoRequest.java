package br.com.saudeConecta.presentation.dto.medico;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarMedicoRequest(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String medNome,
        
        @Size(max = 20, message = "Sexo deve ter no máximo 20 caracteres")
        String medSexo,
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate medDataNacimento,
        
        @Size(max = 20, message = "CRM deve ter no máximo 20 caracteres")
        String medCrm,
        
        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String medCpf,
        
        @Size(max = 20, message = "RG deve ter no máximo 20 caracteres")
        String medRg,
        
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String medEmail,
        
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String medTelefone,
        
        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        String medEspecialidade,
        
        @Size(max = 500, message = "Formações deve ter no máximo 500 caracteres")
        String medFormacoes,
        
        @Size(max = 100, message = "Empresa deve ter no máximo 100 caracteres")
        String medEmpresa,
        
        @Size(max = 100, message = "Graduação deve ter no máximo 100 caracteres")
        String medGraduacao,
        
        @Size(max = 50, message = "Tempo de consulta deve ter no máximo 50 caracteres")
        String medTempoDeConsulta,
        
        // Dados do endereço
        String endNacionalidade,
        String endUF,
        String endMunicipio,
        String endBairro,
        String endCep,
        String endRua,
        Integer endNumero,
        String endComplemento
) {
}
