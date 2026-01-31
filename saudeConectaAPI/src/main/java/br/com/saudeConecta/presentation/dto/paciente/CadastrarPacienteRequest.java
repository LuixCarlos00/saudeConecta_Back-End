package br.com.saudeConecta.presentation.dto.paciente;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastrarPacienteRequest(
        @NotBlank(message = "Nome não pode ser vazio")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String paciNome,
        
        @NotBlank(message = "Sexo não pode ser vazio")
        @Size(max = 1, message = "Sexo deve ter no máximo 1 caractere")
        String paciSexo,
        
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate paciDataNacimento,
        
        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String paciCpf,
        
        @Size(max = 20, message = "RG deve ter no máximo 20 caracteres")
        String paciRg,
        
        @NotBlank(message = "Email não pode ser vazio")
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String paciEmail,
        
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String paciTelefone,
        
        @NotBlank(message = "Status não pode ser vazio")
        @Size(max = 20, message = "Status deve ter no máximo 20 caracteres")
        String paciStatus,
        
        @NotNull(message = "ID do endereço não pode ser nulo")
        @JsonProperty("Endereco")
        Long endereco
) {
}
