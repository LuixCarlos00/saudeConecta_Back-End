package br.com.saudeConecta.presentation.dto.paciente;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastrarPacienteCompletoRequest(
        // Dados do Paciente
        @NotBlank(message = "Nome não pode ser vazio")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String paciNome,
        
        @NotBlank(message = "Sexo não pode ser vazio")
        String paciSexo,
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate paciDataNacimento,
        
        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String paciCpf,
        
        @Size(max = 12, message = "RG deve ter no máximo 12 caracteres")
        String paciRg,
        
        @NotBlank(message = "Email não pode ser vazio")
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String paciEmail,
        
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String paciTelefone,
        
        // Dados do Endereço
        @NotBlank(message = "Nacionalidade não pode ser vazia")
        @Size(max = 50, message = "Nacionalidade deve ter no máximo 50 caracteres")
        String endNacionalidade,
        
        @Size(max = 2, message = "UF deve ter no máximo 2 caracteres")
        String endUF,
        
        @NotBlank(message = "Município não pode ser vazio")
        @Size(max = 100, message = "Município deve ter no máximo 100 caracteres")
        String endMunicipio,
        
        @NotBlank(message = "Bairro não pode ser vazio")
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
        String endBairro,
        
        @NotBlank(message = "CEP não pode ser vazio")
        @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
        String endCep,
        
        @NotBlank(message = "Rua não pode ser vazia")
        @Size(max = 200, message = "Rua deve ter no máximo 200 caracteres")
        String endRua,
        
        Integer endNumero,
        
        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
        String endComplemento
) {
}
