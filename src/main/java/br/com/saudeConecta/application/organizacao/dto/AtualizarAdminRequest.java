package br.com.saudeConecta.application.organizacao.dto;

public record AtualizarAdminRequest(
        String nome,
        String email
) {}