package br.com.saudeConecta.application.secretaria.dto;

import br.com.saudeConecta.config.web.SafeInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Dados aceitos na atualização de uma secretária. Só nome, e-mail e telefone podem mudar.
 * O corpo antes era a própria entidade, e qualquer campo dela podia chegar na requisição.
 * Campo nulo significa "não altera", por isso nenhum é obrigatório.
 *
 * @param nome     novo nome (opcional)
 * @param email    novo e-mail (opcional; precisa ser livre na base)
 * @param telefone novo telefone (opcional)
 */
public record AtualizarSecretariaRequest(
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @SafeInput
    String nome,

    @Email(message = "Email inválido")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    String email,

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String telefone
) {}
