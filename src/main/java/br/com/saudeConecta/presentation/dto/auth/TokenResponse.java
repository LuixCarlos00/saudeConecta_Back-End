package br.com.saudeConecta.presentation.dto.auth;

public record TokenResponse(
    String token,
    Long userId,
    String login,
    Long organizacaoId,
    String tipoUsuario
) {}
