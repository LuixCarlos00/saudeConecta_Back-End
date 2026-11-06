package br.com.saudeConecta.application.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record BloquearUsuarioRequest(
    @NotNull(message = "Código do usuário é obrigatório")
    Long codigo,
    
    @NotNull(message = "Código do usuário é obrigatório") 
    Long codigoUsuario,
    
    @NotNull(message = "Status é obrigatório")
    Integer status
) {}
