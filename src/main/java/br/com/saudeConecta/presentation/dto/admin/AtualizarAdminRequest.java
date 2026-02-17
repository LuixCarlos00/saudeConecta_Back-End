package br.com.saudeConecta.presentation.dto.admin;

public record AtualizarAdminRequest(
        String nome,
        String email
) {}