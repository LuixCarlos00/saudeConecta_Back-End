package br.com.saudeConecta.presentation.dto.secretaria;

import br.com.saudeConecta.util.validation.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarSecretariaRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @SafeInput
    String nome,

    @NotBlank(message = "CPF é obrigatório")
    @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
    String cpf,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String telefone
) {}
