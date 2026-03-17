package br.com.saudeConecta.presentation.dto.profissional;

import br.com.saudeConecta.util.validation.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarClinicoRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @SafeInput
    String nome,

    @Size(max = 20, message = "Sexo deve ter no máximo 20 caracteres")
    String sexo,

    String dataNascimento,

    @NotBlank(message = "CRM é obrigatório")
    @Size(max = 20, message = "Registro deve ter no máximo 20 caracteres")
    @SafeInput
    String registroConselho,

    @NotBlank(message = "CPF é obrigatório")
    @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
    String cpf,

    @Size(max = 12, message = "RG deve ter no máximo 12 caracteres")
    String rg,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String telefone,

    @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
    @SafeInput
    String especialidade,

    @Size(max = 100, message = "Formação deve ter no máximo 100 caracteres")
    @SafeInput
    String formacao,

    @Size(max = 100, message = "Instituição deve ter no máximo 100 caracteres")
    @SafeInput
    String instituicao,

    Integer tempoConsultaMinutos,

    @Size(max = 20, message = "Tipo profissional deve ter no máximo 20 caracteres")
    String tipoProfissional,

    @Size(max = 50, message = "Nacionalidade deve ter no máximo 50 caracteres")
    @SafeInput
    String nacionalidade,

    @Size(max = 2, message = "UF deve ter no máximo 2 caracteres")
    String uf,

    @Size(max = 100, message = "Município deve ter no máximo 100 caracteres")
    @SafeInput
    String municipio,

    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    @SafeInput
    String bairro,

    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    String cep,

    @Size(max = 200, message = "Rua deve ter no máximo 200 caracteres")
    @SafeInput
    String rua,

    Integer numero,

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    @SafeInput
    String complemento
) {}
