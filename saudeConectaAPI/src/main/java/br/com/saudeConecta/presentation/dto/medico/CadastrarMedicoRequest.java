package br.com.saudeConecta.presentation.dto.medico;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastrarMedicoRequest(
        @NotBlank(message = "Nome não pode ser vazio")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String medNome,
        
        @NotBlank(message = "Sexo não pode ser vazio")
        @Size(max = 1, message = "Sexo deve ter no máximo 1 caractere")
        String medSexo,
        
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate medDataNacimento,
        
        @NotBlank(message = "CRM não pode ser vazio")
        @Size(max = 20, message = "CRM deve ter no máximo 20 caracteres")
        String medCrm,
        
        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String medCpf,
        
        @Size(max = 20, message = "RG deve ter no máximo 20 caracteres")
        String medRg,
        
        @NotBlank(message = "Email não pode ser vazio")
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
        
        @NotNull(message = "ID do usuário não pode ser nulo")
        @JsonProperty("Usuario")
        Long usuario,
        
        @NotNull(message = "ID do endereço não pode ser nulo")
        @JsonProperty("Endereco")
        Long endereco
) {
}
