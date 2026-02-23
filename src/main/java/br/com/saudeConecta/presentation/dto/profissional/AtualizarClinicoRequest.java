package br.com.saudeConecta.presentation.dto.profissional;

import br.com.saudeConecta.presentation.dto.endereco.EnderecoResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AtualizarClinicoRequest(

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "CPF é obrigatório")
        String cpf,

        String rg,

        @NotBlank(message = "Registro do conselho é obrigatório")
        String registroConselho,

        String telefone,           // ← removido @NotBlank, telefone é opcional

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotNull(message = "Sexo é obrigatório")
        String sexo,               // ← adicionar sexo que estava faltando

        @NotNull(message = "Data de nascimento é obrigatória")
        LocalDate dataNascimento,

        String formacao,

        String instituicao,

        Integer tempoConsultaMinutos,

        BigDecimal valorConsulta,

        String tipoProfissional,

        String especialidade,

        EnderecoResponse endereco
) {}