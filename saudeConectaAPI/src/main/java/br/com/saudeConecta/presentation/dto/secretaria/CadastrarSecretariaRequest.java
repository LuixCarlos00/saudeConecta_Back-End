package br.com.saudeConecta.presentation.dto.secretaria;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastrarSecretariaRequest(
    @NotBlank(message = "Nome é obrigatório")
    String secreNome,
    
    @NotBlank(message = "CPF é obrigatório")
    String secreCpf,
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String secreEmail,
    
    String secreCodigoAutorizacao
) {}
