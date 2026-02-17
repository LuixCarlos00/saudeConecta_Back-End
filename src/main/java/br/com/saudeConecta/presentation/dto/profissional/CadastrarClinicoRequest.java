package br.com.saudeConecta.presentation.dto.profissional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastrarClinicoRequest(
    @NotBlank(message = "Nome é obrigatório")
    String nome,
    
    String sexo,
    
    String dataNascimento,
    
    @NotBlank(message = "CRM é obrigatório")
    String registroConselho,
    
    @NotBlank(message = "CPF é obrigatório")
    String cpf,
    
    String rg,
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String email,
    
    String telefone,
    
    String especialidade,
    
    String formacao,
    
    String instituicao,
    
    Integer tempoConsultaMinutos,
    
    String nacionalidade,
    String uf,
    String municipio,
    String bairro,
    String cep,
    String rua,
    Integer numero,
    String complemento
) {}
