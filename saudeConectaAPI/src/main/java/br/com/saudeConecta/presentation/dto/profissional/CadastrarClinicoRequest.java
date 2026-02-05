package br.com.saudeConecta.presentation.dto.profissional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastrarClinicoRequest(
    @NotBlank(message = "Nome é obrigatório")
    String medNome,
    
    String medSexo,
    
    String medDataNacimento,
    
    @NotBlank(message = "CRM é obrigatório")
    String medCrm,
    
    @NotBlank(message = "CPF é obrigatório")
    String medCpf,
    
    String medRg,
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String medEmail,
    
    String medTelefone,
    
    String medEspecialidade,
    
    String endNacionalidade,
    String endUF,
    String endMunicipio,
    String endBairro,
    String endCep,
    String endRua,
    Integer endNumero,
    String endComplemento
) {}
