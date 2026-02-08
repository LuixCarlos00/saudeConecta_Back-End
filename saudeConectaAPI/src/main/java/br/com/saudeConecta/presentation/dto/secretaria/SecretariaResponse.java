package br.com.saudeConecta.presentation.dto.secretaria;

import br.com.saudeConecta.domain.secretaria.Secretaria;

public record SecretariaResponse(
    Long codigo,
    String nome,
    String cpf,
    String email,
    String telefone,
    String status,
    Long usuarioId
) {
    public static SecretariaResponse fromEntity(Secretaria secretaria) {
        return new SecretariaResponse(
            secretaria.getId(),
            secretaria.getNome(),
            secretaria.getCpf(),
            secretaria.getEmail(),
            secretaria.getTelefone(),
            secretaria.getStatus().name(),
            secretaria.getUsuario() != null ? secretaria.getUsuario().getId() : null
        );
    }
}
