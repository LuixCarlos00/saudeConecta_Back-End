package br.com.saudeConecta.presentation.dto.profissional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CadastrarProfissionalRequest(
    @NotBlank(message = "Nome é obrigatório")
    String nome,
    
    @NotNull(message = "Tipo de profissional é obrigatório")
    Long tipoProfissionalId,
    
    String sexo,
    
    LocalDate dataNascimento,
    
    @NotBlank(message = "Registro do conselho é obrigatório")
    String registroConselho,
    
    String cpf,
    
    String rg,
    
    @Email(message = "Email inválido")
    String email,
    
    String telefone,
    
    String formacao,
    
    String instituicao,
    
    Integer tempoConsultaMinutos,
    
    Set<Long> especialidadeIds,
    
    String endNacionalidade,
    String endUF,
    String endMunicipio,
    String endBairro,
    String endCep,
    String endRua,
    Long endNumero,
    String endComplemento
) {}
