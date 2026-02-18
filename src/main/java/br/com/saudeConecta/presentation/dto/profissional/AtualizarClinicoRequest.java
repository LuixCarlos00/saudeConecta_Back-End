package br.com.saudeConecta.presentation.dto.profissional;

import br.com.saudeConecta.presentation.dto.endereco.EnderecoResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarClinicoRequest(
    @NotBlank(message = "Nome é obrigatório")
    String nome,
    
    @NotBlank(message = "CPF é obrigatório")
    String cpf,
    
    String rg,
    
    @NotBlank(message = "Registro do conselho é obrigatório")
    String registroConselho,
    
    @NotBlank(message = "Telefone é obrigatório")
    String telefone,
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String email,
    
    LocalDate dataNascimento,
    
    String formacao,
    
    String instituicao,
    
    Integer tempoConsultaMinutos,
    
    BigDecimal valorConsulta,
    
    String tipoProfissional,
    
    String especialidade,
    
    EnderecoResponse endereco
) {
}
