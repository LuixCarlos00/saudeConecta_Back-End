package br.com.saudeConecta.presentation.dto.usuario;

import jakarta.validation.constraints.NotNull;

public record BloquearUsuarioRequest(
    @NotNull(message = "Código do usuário é obrigatório")
    Long codigo,
    
    @NotNull(message = "Código do usuário é obrigatório") 
    Long codigoUsuario,
    
    @NotNull(message = "Status é obrigatório")
    Integer status
) {}
