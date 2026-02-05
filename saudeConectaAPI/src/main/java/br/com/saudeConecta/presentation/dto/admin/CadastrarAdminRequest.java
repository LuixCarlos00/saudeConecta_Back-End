package br.com.saudeConecta.presentation.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastrarAdminRequest(
    @NotBlank(message = "Nome é obrigatório")
    String admNome,
    
    @NotBlank(message = "CPF é obrigatório")
    String admCpf,
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String admEmail,
    
    String admCodigoAutorizacao
) {}
