package br.com.saudeConecta.presentation.dto.profissional;

import br.com.saudeConecta.presentation.dto.endereco.EnderecoResponse;
import br.com.saudeConecta.util.validation.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AtualizarClinicoRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        @SafeInput
        String nome,

        @NotBlank(message = "CPF é obrigatório")
        @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres")
        String cpf,

        @Size(max = 12, message = "RG deve ter no máximo 12 caracteres")
        String rg,

        @NotBlank(message = "Registro do conselho é obrigatório")
        @Size(max = 20, message = "Registro deve ter no máximo 20 caracteres")
        @SafeInput
        String registroConselho,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @NotNull(message = "Sexo é obrigatório")
        @Size(max = 20, message = "Sexo deve ter no máximo 20 caracteres")
        String sexo,

        @NotNull(message = "Data de nascimento é obrigatória")
        LocalDate dataNascimento,

        @Size(max = 100, message = "Formação deve ter no máximo 100 caracteres")
        @SafeInput
        String formacao,

        @Size(max = 100, message = "Instituição deve ter no máximo 100 caracteres")
        @SafeInput
        String instituicao,

        Integer tempoConsultaMinutos,

        BigDecimal valorConsulta,

        @Size(max = 20, message = "Tipo profissional deve ter no máximo 20 caracteres")
        String tipoProfissional,

        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        @SafeInput
        String especialidade,

        EnderecoResponse endereco
) {}